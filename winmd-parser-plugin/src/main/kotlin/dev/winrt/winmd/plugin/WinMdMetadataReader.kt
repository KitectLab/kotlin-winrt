package dev.winrt.winmd.plugin

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object WinMdMetadataReader {
    private const val peSignature = 0x00004550
    private const val cliDirectoryIndex = 14
    private const val metadataSignature = 0x424A5342
    private const val tableTypeRef = 1
    private const val tableTypeDef = 2
    private const val tableField = 4
    private const val tableMethodDef = 6
    private const val tableTypeSpec = 27

    fun readModel(sourceFiles: List<Path>): WinMdModel {
        val fileInfo = sourceFiles.map { path ->
            val peInfo = PortableExecutableReader.inspect(path)
            WinMdFile(
                path = path.toString(),
                size = peInfo.size,
                portableExecutable = peInfo.isPortableExecutable,
            )
        }

        val types = sourceFiles.flatMap(::readTypes)
        val namespaces = types.groupBy(WinMdType::namespace)
            .toSortedMap()
            .map { (namespace, namespaceTypes) ->
                WinMdNamespace(
                    name = namespace,
                    types = namespaceTypes.sortedBy(WinMdType::name),
                )
            }

        return WinMdModel(
            files = fileInfo,
            namespaces = namespaces,
        )
    }

    fun readTypes(path: Path): List<WinMdType> {
        val bytes = Files.readAllBytes(path)
        val reader = WinMdBinaryReader(bytes)
        val tables = reader.readMetadataTables()

        return tables.typeDefs
            .filterNot { it.name == "<Module>" }
            .map { typeDef ->
                WinMdType(
                    namespace = typeDef.namespace,
                    name = typeDef.name,
                    kind = classifyType(typeDef, tables),
                )
            }
    }

    private fun classifyType(typeDef: TypeDefRow, tables: MetadataTables): WinMdTypeKind {
        if ((typeDef.flags and 0x20) != 0) {
            return WinMdTypeKind.Interface
        }

        val extends = resolveExtends(typeDef.extendsCodedIndex, tables) ?: return WinMdTypeKind.RuntimeClass
        return when {
            extends.namespace == "System" && extends.name == "Enum" -> WinMdTypeKind.Enum
            extends.namespace == "System" && extends.name == "ValueType" -> WinMdTypeKind.Struct
            else -> WinMdTypeKind.RuntimeClass
        }
    }

    private fun resolveExtends(codedIndex: Int, tables: MetadataTables): TypeReferenceRow? {
        if (codedIndex == 0) {
            return null
        }

        val tag = codedIndex and 0x3
        val index = codedIndex ushr 2
        return when (tag) {
            1 -> tables.typeRefs.getOrNull(index - 1)
            0, 2 -> null
            else -> null
        }
    }

    private class WinMdBinaryReader(private val bytes: ByteArray) {
        private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        fun readMetadataTables(): MetadataTables {
            val peOffset = readInt32(0x3C)
            require(readInt32(peOffset) == peSignature) {
                "Not a valid PE image"
            }

            val sectionCount = readUInt16(peOffset + 6)
            val optionalHeaderSize = readUInt16(peOffset + 20)
            val optionalHeaderOffset = peOffset + 24
            val magic = readUInt16(optionalHeaderOffset)
            val dataDirectoryOffset = optionalHeaderOffset + if (magic == 0x20B) 112 else 96
            val cliDirectoryEntryOffset = dataDirectoryOffset + (cliDirectoryIndex * 8)
            val cliRva = readInt32(cliDirectoryEntryOffset)
            val sectionHeadersOffset = optionalHeaderOffset + optionalHeaderSize
            val sections = (0 until sectionCount).map { index ->
                val sectionOffset = sectionHeadersOffset + (index * 40)
                SectionHeader(
                    virtualAddress = readInt32(sectionOffset + 12),
                    virtualSize = readInt32(sectionOffset + 8),
                    rawDataSize = readInt32(sectionOffset + 16),
                    rawDataPointer = readInt32(sectionOffset + 20),
                )
            }

            val cliOffset = rvaToOffset(cliRva, sections)
            val metadataRva = readInt32(cliOffset + 8)
            val metadataOffset = rvaToOffset(metadataRva, sections)
            return readMetadataRoot(metadataOffset)
        }

        private fun readMetadataRoot(metadataOffset: Int): MetadataTables {
            require(readInt32(metadataOffset) == metadataSignature) {
                "Not a valid CLI metadata root"
            }

            val versionLength = readInt32(metadataOffset + 12)
            val streamCount = readUInt16(align4(metadataOffset + 16 + versionLength) + 2)
            var cursor = align4(metadataOffset + 16 + versionLength) + 4

            var stringHeap: ByteBuffer? = null
            var tablesHeap: ByteBuffer? = null
            repeat(streamCount) {
                val offset = readInt32(cursor)
                val size = readInt32(cursor + 4)
                val name = readZeroTerminatedString(cursor + 8)
                val headerSize = align4(8 + name.length + 1)
                when (name) {
                    "#Strings" -> stringHeap = slice(metadataOffset + offset, size)
                    "#~" -> tablesHeap = slice(metadataOffset + offset, size)
                }
                cursor += headerSize
            }

            return readTables(
                tablesHeap ?: error("Missing #~ stream"),
                stringHeap ?: error("Missing #Strings stream"),
            )
        }

        private fun readTables(tablesHeap: ByteBuffer, stringHeap: ByteBuffer): MetadataTables {
            tablesHeap.order(ByteOrder.LITTLE_ENDIAN)
            val heapSizes = tablesHeap.get(6).toInt() and 0xFF
            val stringIndexSize = if ((heapSizes and 0x01) != 0) 4 else 2
            val validMask = tablesHeap.getLong(8)
            var cursor = 24
            val rowCounts = IntArray(64)
            for (tableId in 0 until 64) {
                if (((validMask ushr tableId) and 1L) != 0L) {
                    rowCounts[tableId] = tablesHeap.getInt(cursor)
                    cursor += 4
                }
            }

            val typeRefRows = mutableListOf<TypeReferenceRow>()
            val typeDefRows = mutableListOf<TypeDefRow>()
            val fieldIndexSize = tableIndexSize(rowCounts[tableField])
            val methodIndexSize = tableIndexSize(rowCounts[tableMethodDef])
            val typeDefOrRefIndexSize = codedIndexSize(
                rowCounts[tableTypeDef],
                rowCounts[tableTypeRef],
                rowCounts[tableTypeSpec],
            )

            for (tableId in 0 until 64) {
                val rowCount = rowCounts[tableId]
                if (rowCount == 0) {
                    continue
                }

                when (tableId) {
                    tableTypeRef -> {
                        val resolutionScopeSize = codedIndexSize(
                            rowCounts[0],
                            rowCounts[26],
                            rowCounts[35],
                            rowCounts[1],
                        )
                        repeat(rowCount) {
                            cursor += resolutionScopeSize
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            val namespace = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            typeRefRows += TypeReferenceRow(namespace = namespace, name = name)
                        }
                    }
                    tableTypeDef -> {
                        repeat(rowCount) {
                            val flags = tablesHeap.getInt(cursor)
                            cursor += 4
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            val namespace = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            val extendsIndex = readIndex(tablesHeap, cursor, typeDefOrRefIndexSize)
                            cursor += typeDefOrRefIndexSize
                            cursor += fieldIndexSize
                            cursor += methodIndexSize
                            typeDefRows += TypeDefRow(
                                namespace = namespace,
                                name = name,
                                flags = flags,
                                extendsCodedIndex = extendsIndex,
                            )
                        }
                    }
                    else -> {
                        cursor += rowCount * rowSize(tableId, rowCounts, stringIndexSize, heapSizes)
                    }
                }
            }

            return MetadataTables(
                typeRefs = typeRefRows,
                typeDefs = typeDefRows,
            )
        }

        private fun rowSize(tableId: Int, rowCounts: IntArray, stringIndexSize: Int, heapSizes: Int): Int {
            val guidIndexSize = if ((heapSizes and 0x02) != 0) 4 else 2
            val blobIndexSize = if ((heapSizes and 0x04) != 0) 4 else 2
            val fieldIndexSize = tableIndexSize(rowCounts[tableField])
            val methodIndexSize = tableIndexSize(rowCounts[tableMethodDef])
            return when (tableId) {
                0 -> 2 + stringIndexSize + guidIndexSize * 3
                3 -> fieldIndexSize
                4 -> 2 + stringIndexSize + blobIndexSize
                5 -> 8 + stringIndexSize + blobIndexSize + tableIndexSize(rowCounts[8])
                6 -> tableIndexSize(rowCounts[6]) + codedIndexSize(rowCounts[20], rowCounts[23]) + blobIndexSize
                8 -> 2
                9 -> tableIndexSize(rowCounts[2]) + codedIndexSize(
                    rowCounts[2],
                    rowCounts[1],
                    rowCounts[27],
                )
                10 -> codedIndexSize(rowCounts[2], rowCounts[1], rowCounts[27]) + stringIndexSize + blobIndexSize
                11 -> 2 + 2 + codedIndexSize(rowCounts[4], rowCounts[8], rowCounts[23]) + stringIndexSize
                12 -> codedIndexSize(rowCounts[6], rowCounts[10]) + codedIndexSize(rowCounts[4], rowCounts[8], rowCounts[23])
                13 -> codedIndexSize(rowCounts[4], rowCounts[8], rowCounts[23]) + blobIndexSize
                14 -> 2 + codedIndexSize(rowCounts[2], rowCounts[6], rowCounts[32]) + blobIndexSize
                15 -> 2 + 4 + tableIndexSize(rowCounts[2])
                16 -> 4 + tableIndexSize(rowCounts[4])
                17 -> blobIndexSize
                18 -> tableIndexSize(rowCounts[2]) + tableIndexSize(rowCounts[20])
                20 -> 2 + stringIndexSize + codedIndexSize(rowCounts[2], rowCounts[1], rowCounts[27])
                21 -> tableIndexSize(rowCounts[2]) + tableIndexSize(rowCounts[23])
                23 -> 2 + stringIndexSize + blobIndexSize
                24 -> 2 + tableIndexSize(rowCounts[6]) + codedIndexSize(rowCounts[20], rowCounts[23])
                25 -> tableIndexSize(rowCounts[20])
                26 -> stringIndexSize
                27 -> blobIndexSize
                28 -> 2 + codedIndexSize(rowCounts[4], rowCounts[6], rowCounts[2], rowCounts[1], rowCounts[20], rowCounts[23], rowCounts[26], rowCounts[27], rowCounts[32], rowCounts[35], rowCounts[38], rowCounts[39], rowCounts[40], rowCounts[42], rowCounts[44], rowCounts[43]) + stringIndexSize + tableIndexSize(rowCounts[26])
                29 -> 4 + tableIndexSize(rowCounts[4])
                32 -> 4 + 4 + stringIndexSize + blobIndexSize + tableIndexSize(rowCounts[8])
                35 -> 2 + 2 + 2 + 2 + 4 + blobIndexSize + stringIndexSize + stringIndexSize + tableIndexSize(rowCounts[0])
                38 -> 4 + stringIndexSize + blobIndexSize
                39 -> 4 + tableIndexSize(rowCounts[2]) + stringIndexSize + blobIndexSize
                40 -> 2 + 2 + stringIndexSize + tableIndexSize(rowCounts[23])
                41 -> tableIndexSize(rowCounts[2]) + codedIndexSize(rowCounts[4], rowCounts[6])
                42 -> 2 + 2 + stringIndexSize + blobIndexSize
                43 -> tableIndexSize(rowCounts[42])
                44 -> codedIndexSize(rowCounts[4], rowCounts[6]) + codedIndexSize(rowCounts[4], rowCounts[6], rowCounts[2], rowCounts[1], rowCounts[20], rowCounts[23], rowCounts[26], rowCounts[27], rowCounts[32], rowCounts[35], rowCounts[38], rowCounts[39], rowCounts[40], rowCounts[42], rowCounts[44], rowCounts[43]) + blobIndexSize
                else -> 0
            }
        }

        private fun tableIndexSize(rowCount: Int): Int = if (rowCount < 0x10000) 2 else 4

        private fun codedIndexSize(vararg rowCounts: Int): Int {
            val tagBits = when (rowCounts.size) {
                2 -> 1
                3 -> 2
                4 -> 2
                5 -> 3
                16 -> 4
                else -> error("Unsupported coded index width for ${rowCounts.size} tables")
            }
            val maxRowCount = rowCounts.maxOrNull() ?: 0
            return if (maxRowCount < (1 shl (16 - tagBits))) 2 else 4
        }

        private fun readStringIndex(
            tablesHeap: ByteBuffer,
            stringHeap: ByteBuffer,
            offset: Int,
            size: Int,
        ): String {
            val index = readIndex(tablesHeap, offset, size)
            if (index == 0) {
                return ""
            }

            val start = index
            var end = start
            while (end < stringHeap.limit() && stringHeap.get(end) != 0.toByte()) {
                end++
            }
            val bytes = ByteArray(end - start)
            stringHeap.position(start)
            stringHeap.get(bytes)
            return bytes.toString(StandardCharsets.UTF_8)
        }

        private fun readIndex(buffer: ByteBuffer, offset: Int, size: Int): Int {
            return if (size == 2) {
                buffer.getShort(offset).toInt() and 0xFFFF
            } else {
                buffer.getInt(offset)
            }
        }

        private fun readZeroTerminatedString(offset: Int): String {
            var cursor = offset
            while (bytes[cursor] != 0.toByte()) {
                cursor++
            }
            return bytes.copyOfRange(offset, cursor).toString(StandardCharsets.UTF_8)
        }

        private fun slice(offset: Int, size: Int): ByteBuffer {
            val source = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            source.position(offset)
            source.limit(offset + size)
            return source.slice().order(ByteOrder.LITTLE_ENDIAN)
        }

        private fun rvaToOffset(rva: Int, sections: List<SectionHeader>): Int {
            val section = sections.firstOrNull { candidate ->
                rva >= candidate.virtualAddress && rva < candidate.virtualAddress + maxOf(candidate.virtualSize, candidate.rawDataSize)
            } ?: error("Unable to map RVA 0x${rva.toString(16)} to file offset")
            return section.rawDataPointer + (rva - section.virtualAddress)
        }

        private fun readInt32(offset: Int): Int = buffer.getInt(offset)

        private fun readUInt16(offset: Int): Int = buffer.getShort(offset).toInt() and 0xFFFF

        private fun align4(value: Int): Int = (value + 3) and -4
    }
}
