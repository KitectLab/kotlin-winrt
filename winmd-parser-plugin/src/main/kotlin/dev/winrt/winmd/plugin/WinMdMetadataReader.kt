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
    private const val tableParam = 8
    private const val tableMemberRef = 10
    private const val tableCustomAttribute = 12
    private const val tablePropertyMap = 21
    private const val tableProperty = 23
    private const val tableMethodSemantics = 24
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
            .mapIndexedNotNull { index, typeDef ->
                if (typeDef.name == "<Module>") {
                    return@mapIndexedNotNull null
                }
                WinMdType(
                    namespace = typeDef.namespace,
                    name = typeDef.name,
                    kind = classifyType(typeDef, tables),
                    guid = readGuid(index + 1, tables),
                    defaultInterface = readDefaultInterface(index + 1, tables),
                    baseInterfaces = readBaseInterfaces(index + 1, tables),
                    methods = readMethods(index + 1, tables),
                    properties = readProperties(index + 1, tables),
                )
            }
    }

    private fun readGuid(typeDefIndex: Int, tables: MetadataTables): String? {
        val customAttributes = tables.customAttributeRows.filter { decodeHasCustomAttributeTypeDefIndex(it.parentCodedIndex) == typeDefIndex }
        val guidAttribute = customAttributes.firstOrNull { resolveCustomAttributeTypeName(it.typeCodedIndex, tables)?.endsWith(".GuidAttribute") == true }
            ?: return null
        return parseGuidAttributeValue(guidAttribute.value)
    }

    private fun readDefaultInterface(typeDefIndex: Int, tables: MetadataTables): String? {
        val typeKind = classifyType(tables.typeDefs[typeDefIndex - 1], tables)
        if (typeKind != WinMdTypeKind.RuntimeClass) {
            return null
        }

        return tables.interfaceImplRows
            .asSequence()
            .filter { it.classTypeDefIndex == typeDefIndex }
            .map { resolveTypeDefOrRefOrSpecName(it.interfaceCodedIndex, tables) }
            .firstOrNull { it != "UnknownType" }
    }

    private fun readBaseInterfaces(typeDefIndex: Int, tables: MetadataTables): List<String> {
        val typeKind = classifyType(tables.typeDefs[typeDefIndex - 1], tables)
        if (typeKind != WinMdTypeKind.Interface) {
            return emptyList()
        }
        return tables.interfaceImplRows
            .asSequence()
            .filter { it.classTypeDefIndex == typeDefIndex }
            .mapNotNull { resolveTypeDefOrRefOrSpecName(it.interfaceCodedIndex, tables) }
            .filter { it != "UnknownType" }
            .toList()
    }

    private fun decodeHasCustomAttributeTypeDefIndex(codedIndex: Int): Int? {
        val tag = codedIndex and 0x1F
        val index = codedIndex ushr 5
        return if (tag == 3) index else null
    }

    private fun resolveCustomAttributeTypeName(codedIndex: Int, tables: MetadataTables): String? {
        val tag = codedIndex and 0x7
        val index = codedIndex ushr 3
        return when (tag) {
            2 -> null
            3 -> tables.memberRefRows.getOrNull(index - 1)?.let { memberRef ->
                resolveMemberRefParentTypeName(memberRef.classCodedIndex, tables)
            }
            else -> null
        }
    }

    private fun resolveMemberRefParentTypeName(codedIndex: Int, tables: MetadataTables): String? {
        val tag = codedIndex and 0x7
        val index = codedIndex ushr 3
        return when (tag) {
            0 -> tables.typeDefs.getOrNull(index - 1)?.let { qualify(it.namespace, it.name) }
            1 -> tables.typeRefs.getOrNull(index - 1)?.let { qualify(it.namespace, it.name) }
            4 -> null
            else -> null
        }
    }

    private fun parseGuidAttributeValue(blob: ByteArray): String? {
        if (blob.size < 20) {
            return null
        }
        val buffer = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN)
        val prolog = buffer.short.toInt() and 0xFFFF
        if (prolog != 0x0001) {
            return null
        }

        val data1 = buffer.int
        val data2 = buffer.short.toInt() and 0xFFFF
        val data3 = buffer.short.toInt() and 0xFFFF
        val rest = ByteArray(8)
        buffer.get(rest)
        return buildString {
            append(data1.toUInt().toString(16).padStart(8, '0'))
            append('-')
            append(data2.toString(16).padStart(4, '0'))
            append('-')
            append(data3.toString(16).padStart(4, '0'))
            append('-')
            append(rest[0].toUByte().toString(16).padStart(2, '0'))
            append(rest[1].toUByte().toString(16).padStart(2, '0'))
            append('-')
            rest.drop(2).forEach { append(it.toUByte().toString(16).padStart(2, '0')) }
        }
    }

    private fun readMethods(typeDefIndex: Int, tables: MetadataTables): List<WinMdMethod> {
        val typeDef = tables.typeDefs[typeDefIndex - 1]
        val methodStart = typeDef.methodListIndex
        if (methodStart == 0) {
            return emptyList()
        }
        val methodEnd = tables.typeDefs.getOrNull(typeDefIndex)?.methodListIndex ?: (tables.methodDefs.size + 1)
        return (methodStart until methodEnd).mapNotNull { methodIndex ->
            val method = tables.methodDefs.getOrNull(methodIndex - 1) ?: return@mapNotNull null
            val signature = parseMethodSignature(method.signature, tables)
            val params = readMethodParameters(methodIndex, method, tables, signature.parameterTypes.size)
            WinMdMethod(
                name = method.name,
                returnType = signature.returnType,
                parameters = params,
            )
        }
    }

    private fun readMethodParameters(
        methodIndex: Int,
        method: MethodDefRow,
        tables: MetadataTables,
        expectedParameterCount: Int,
    ): List<WinMdParameter> {
        val paramStart = method.paramListIndex
        if (paramStart == 0 || expectedParameterCount == 0) {
            return emptyList()
        }
        val paramEnd = tables.methodDefs.getOrNull(methodIndex)?.paramListIndex ?: (tables.paramRows.size + 1)
        val paramsBySequence = (paramStart until paramEnd)
            .mapNotNull { paramIndex -> tables.paramRows.getOrNull(paramIndex - 1) }
            .filter { it.sequence > 0 }
            .associateBy(ParamRow::sequence)

        val signature = parseMethodSignature(method.signature, tables)
        return signature.parameterTypes.mapIndexed { index, parameterType ->
            val paramRow = paramsBySequence[index + 1]
            WinMdParameter(
                name = paramRow?.name?.takeIf(String::isNotBlank) ?: "p${index + 1}",
                type = parameterType,
            )
        }
    }

    private fun readProperties(typeDefIndex: Int, tables: MetadataTables): List<WinMdProperty> {
        val propertyMap = tables.propertyMapRows.firstOrNull { it.parentTypeDefIndex == typeDefIndex } ?: return emptyList()
        val propertyEnd = tables.propertyMapRows.firstOrNull { it.parentTypeDefIndex > typeDefIndex }?.propertyListIndex
            ?: (tables.propertyRows.size + 1)

        val semanticsByProperty = tables.methodSemanticsRows
            .mapNotNull { semantics ->
                decodeHasSemanticsPropertyIndex(semantics.associationCodedIndex)?.let { propertyIndex ->
                    propertyIndex to semantics.semantics
                }
            }
            .groupBy({ it.first }, { it.second })

        return (propertyMap.propertyListIndex until propertyEnd).mapNotNull { propertyIndex ->
            val property = tables.propertyRows.getOrNull(propertyIndex - 1) ?: return@mapNotNull null
            val propertyType = parsePropertySignature(property.signature, tables)
            val semantics = semanticsByProperty[propertyIndex].orEmpty()
            WinMdProperty(
                name = property.name,
                type = propertyType,
                mutable = semantics.any { it and 0x0001 != 0 },
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

    private fun decodeHasSemanticsPropertyIndex(codedIndex: Int): Int? {
        val tag = codedIndex and 0x1
        val index = codedIndex ushr 1
        return if (tag == 1) index else null
    }

    private fun parseMethodSignature(signature: ByteArray, tables: MetadataTables): ParsedMethodSignature {
        val reader = BlobReader(signature)
        reader.readByte()
        val parameterCount = reader.readCompressedUInt()
        val returnType = parseElementType(reader, tables)
        val parameters = buildList {
            repeat(parameterCount) {
                add(parseElementType(reader, tables))
            }
        }
        return ParsedMethodSignature(
            returnType = returnType,
            parameterTypes = parameters,
        )
    }

    private fun parsePropertySignature(signature: ByteArray, tables: MetadataTables): String {
        val reader = BlobReader(signature)
        reader.readByte()
        reader.readCompressedUInt()
        return parseElementType(reader, tables)
    }

    private fun parseElementType(reader: BlobReader, tables: MetadataTables): String {
        return when (val elementType = reader.readByte()) {
            0x01 -> "Unit"
            0x02 -> "Boolean"
            0x03 -> "Char16"
            0x04 -> "Int8"
            0x05 -> "UInt8"
            0x06 -> "Int16"
            0x07 -> "UInt16"
            0x08 -> "Int32"
            0x09 -> "UInt32"
            0x0A -> "Int64"
            0x0B -> "UInt64"
            0x0C -> "Float32"
            0x0D -> "Float64"
            0x0E -> "String"
            0x11, 0x12 -> resolveTypeDefOrRefOrSpecName(reader.readCompressedUInt(), tables)
            0x15 -> parseGenericInstanceType(reader, tables)
            0x1D -> "${parseElementType(reader, tables)}[]"
            0x1C -> "Object"
            else -> "ElementType0x${elementType.toString(16)}"
        }
    }

    private fun resolveTypeDefOrRefOrSpecName(codedIndex: Int, tables: MetadataTables): String {
        val tag = codedIndex and 0x3
        val index = codedIndex ushr 2
        return when (tag) {
            0 -> tables.typeDefs.getOrNull(index - 1)?.let { qualify(it.namespace, it.name) }
            1 -> tables.typeRefs.getOrNull(index - 1)?.let { qualify(it.namespace, it.name) }
            2 -> tables.typeSpecRows.getOrNull(index - 1)?.let { parseTypeSpecSignature(it.signature, tables) }
            else -> null
        } ?: "UnknownType"
    }

    private fun parseTypeSpecSignature(signature: ByteArray, tables: MetadataTables): String {
        if (signature.isEmpty()) {
            return "UnknownType"
        }
        return try {
            val reader = BlobReader(signature)
            parseElementType(reader, tables)
        } catch (_: IndexOutOfBoundsException) {
            "UnknownType"
        }
    }

    private fun parseGenericInstanceType(reader: BlobReader, tables: MetadataTables): String {
        if (!reader.hasRemaining()) {
            return "UnknownType"
        }
        val next = reader.readByte()
        val genericType = when (next) {
            0x11, 0x12 -> resolveTypeDefOrRefOrSpecName(reader.readCompressedUInt(), tables)
            else -> "ElementType0x${next.toString(16)}"
        }
        val argumentCount = reader.readCompressedUInt()
        val arguments = buildList {
            repeat(argumentCount) {
                add(parseElementType(reader, tables))
            }
        }
        return "$genericType<${arguments.joinToString(", ")}>"
    }

    private fun qualify(namespace: String, name: String): String {
        return if (namespace.isBlank()) name else "$namespace.$name"
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
            var blobHeap: ByteBuffer? = null
            repeat(streamCount) {
                val offset = readInt32(cursor)
                val size = readInt32(cursor + 4)
                val name = readZeroTerminatedString(cursor + 8)
                val headerSize = align4(8 + name.length + 1)
                when (name) {
                    "#Strings" -> stringHeap = slice(metadataOffset + offset, size)
                    "#~" -> tablesHeap = slice(metadataOffset + offset, size)
                    "#Blob" -> blobHeap = slice(metadataOffset + offset, size)
                }
                cursor += headerSize
            }

            return readTables(
                tablesHeap ?: error("Missing #~ stream"),
                stringHeap ?: error("Missing #Strings stream"),
                blobHeap ?: error("Missing #Blob stream"),
            )
        }

        private fun readTables(
            tablesHeap: ByteBuffer,
            stringHeap: ByteBuffer,
            blobHeap: ByteBuffer,
        ): MetadataTables {
            tablesHeap.order(ByteOrder.LITTLE_ENDIAN)
            val heapSizes = tablesHeap.get(6).toInt() and 0xFF
            val stringIndexSize = if ((heapSizes and 0x01) != 0) 4 else 2
            val blobIndexSize = if ((heapSizes and 0x04) != 0) 4 else 2
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
            val typeSpecRows = mutableListOf<TypeSpecRow>()
            val interfaceImplRows = mutableListOf<InterfaceImplRow>()
            val memberRefRows = mutableListOf<MemberRefRow>()
            val customAttributeRows = mutableListOf<CustomAttributeRow>()
            val methodDefRows = mutableListOf<MethodDefRow>()
            val paramRows = mutableListOf<ParamRow>()
            val propertyMapRows = mutableListOf<PropertyMapRow>()
            val propertyRows = mutableListOf<PropertyRow>()
            val methodSemanticsRows = mutableListOf<MethodSemanticsRow>()
            val fieldIndexSize = tableIndexSize(rowCounts[tableField])
            val methodIndexSize = tableIndexSize(rowCounts[tableMethodDef])
            val paramIndexSize = tableIndexSize(rowCounts[tableParam])
            val propertyIndexSize = tableIndexSize(rowCounts[tableProperty])
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
                            val fieldListIndex = readIndex(tablesHeap, cursor, fieldIndexSize)
                            cursor += fieldIndexSize
                            val methodListIndex = readIndex(tablesHeap, cursor, methodIndexSize)
                            cursor += methodIndexSize
                            typeDefRows += TypeDefRow(
                                namespace = namespace,
                                name = name,
                                flags = flags,
                                extendsCodedIndex = extendsIndex,
                                fieldListIndex = fieldListIndex,
                                methodListIndex = methodListIndex,
                            )
                        }
                    }
                    tableTypeSpec -> {
                        repeat(rowCount) {
                            val signature = readBlobIndex(tablesHeap, blobHeap, cursor, blobIndexSize)
                            cursor += blobIndexSize
                            typeSpecRows += TypeSpecRow(signature = signature)
                        }
                    }
                    9 -> {
                        val typeDefIndexSize = tableIndexSize(rowCounts[tableTypeDef])
                        repeat(rowCount) {
                            val classTypeDefIndex = readIndex(tablesHeap, cursor, typeDefIndexSize)
                            cursor += typeDefIndexSize
                            val interfaceCodedIndex = readIndex(
                                tablesHeap,
                                cursor,
                                codedIndexSize(rowCounts[tableTypeDef], rowCounts[tableTypeRef], rowCounts[tableTypeSpec]),
                            )
                            cursor += codedIndexSize(rowCounts[tableTypeDef], rowCounts[tableTypeRef], rowCounts[tableTypeSpec])
                            interfaceImplRows += InterfaceImplRow(
                                classTypeDefIndex = classTypeDefIndex,
                                interfaceCodedIndex = interfaceCodedIndex,
                            )
                        }
                    }
                    tableMemberRef -> {
                        val memberRefParentSize = codedIndexSize(
                            rowCounts[tableTypeDef],
                            rowCounts[tableTypeRef],
                            rowCounts[26],
                            rowCounts[6],
                            rowCounts[tableTypeSpec],
                        )
                        repeat(rowCount) {
                            val classCodedIndex = readIndex(tablesHeap, cursor, memberRefParentSize)
                            cursor += memberRefParentSize
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            cursor += blobIndexSize
                            memberRefRows += MemberRefRow(
                                classCodedIndex = classCodedIndex,
                                name = name,
                            )
                        }
                    }
                    tableCustomAttribute -> {
                        val parentSize = codedIndexSize(
                            rowCounts[tableMethodDef],
                            rowCounts[tableField],
                            rowCounts[tableTypeRef],
                            rowCounts[tableTypeDef],
                            rowCounts[tableParam],
                            rowCounts[9],
                            rowCounts[tableMemberRef],
                            rowCounts[0],
                            rowCounts[tableProperty],
                            rowCounts[20],
                            rowCounts[17],
                            rowCounts[26],
                            rowCounts[tableTypeSpec],
                            rowCounts[32],
                            rowCounts[35],
                            rowCounts[38],
                            rowCounts[39],
                            rowCounts[40],
                            rowCounts[42],
                            rowCounts[44],
                            rowCounts[43],
                        )
                        val typeSize = codedIndexSize(rowCounts[tableMethodDef], rowCounts[tableMemberRef])
                        repeat(rowCount) {
                            val parentCodedIndex = readIndex(tablesHeap, cursor, parentSize)
                            cursor += parentSize
                            val typeCodedIndex = readIndex(tablesHeap, cursor, typeSize)
                            cursor += typeSize
                            val value = readBlobIndex(tablesHeap, blobHeap, cursor, blobIndexSize)
                            cursor += blobIndexSize
                            customAttributeRows += CustomAttributeRow(
                                parentCodedIndex = parentCodedIndex,
                                typeCodedIndex = typeCodedIndex,
                                value = value,
                            )
                        }
                    }
                    tableMethodDef -> {
                        repeat(rowCount) {
                            cursor += 4
                            cursor += 2
                            cursor += 2
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            val signature = readBlobIndex(tablesHeap, blobHeap, cursor, blobIndexSize)
                            cursor += blobIndexSize
                            val paramListIndex = readIndex(tablesHeap, cursor, paramIndexSize)
                            cursor += paramIndexSize
                            methodDefRows += MethodDefRow(
                                name = name,
                                signature = signature,
                                paramListIndex = paramListIndex,
                            )
                        }
                    }
                    tableParam -> {
                        repeat(rowCount) {
                            cursor += 2
                            val sequence = readUInt16(tablesHeap, cursor)
                            cursor += 2
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            paramRows += ParamRow(sequence = sequence, name = name)
                        }
                    }
                    tablePropertyMap -> {
                        val typeDefIndexSize = tableIndexSize(rowCounts[tableTypeDef])
                        repeat(rowCount) {
                            val parentTypeDefIndex = readIndex(tablesHeap, cursor, typeDefIndexSize)
                            cursor += typeDefIndexSize
                            val propertyListIndex = readIndex(tablesHeap, cursor, propertyIndexSize)
                            cursor += propertyIndexSize
                            propertyMapRows += PropertyMapRow(
                                parentTypeDefIndex = parentTypeDefIndex,
                                propertyListIndex = propertyListIndex,
                            )
                        }
                    }
                    tableProperty -> {
                        repeat(rowCount) {
                            cursor += 2
                            val name = readStringIndex(tablesHeap, stringHeap, cursor, stringIndexSize)
                            cursor += stringIndexSize
                            val signature = readBlobIndex(tablesHeap, blobHeap, cursor, blobIndexSize)
                            cursor += blobIndexSize
                            propertyRows += PropertyRow(name = name, signature = signature)
                        }
                    }
                    tableMethodSemantics -> {
                        val associationSize = codedIndexSize(rowCounts[20], rowCounts[23])
                        repeat(rowCount) {
                            val semantics = readUInt16(tablesHeap, cursor)
                            cursor += 2
                            val methodIndex = readIndex(tablesHeap, cursor, methodIndexSize)
                            cursor += methodIndexSize
                            val associationCodedIndex = readIndex(tablesHeap, cursor, associationSize)
                            cursor += associationSize
                            methodSemanticsRows += MethodSemanticsRow(
                                semantics = semantics,
                                methodIndex = methodIndex,
                                associationCodedIndex = associationCodedIndex,
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
                typeSpecRows = typeSpecRows,
                interfaceImplRows = interfaceImplRows,
                memberRefRows = memberRefRows,
                customAttributeRows = customAttributeRows,
                methodDefs = methodDefRows,
                paramRows = paramRows,
                propertyMapRows = propertyMapRows,
                propertyRows = propertyRows,
                methodSemanticsRows = methodSemanticsRows,
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
                5 -> methodIndexSize
                6 -> tableIndexSize(rowCounts[6]) + codedIndexSize(rowCounts[20], rowCounts[23]) + blobIndexSize
                7 -> tableIndexSize(rowCounts[8])
                8 -> 2 + 2 + stringIndexSize
                9 -> tableIndexSize(rowCounts[2]) + codedIndexSize(
                    rowCounts[2],
                    rowCounts[1],
                    rowCounts[27],
                )
                10 -> codedIndexSize(rowCounts[2], rowCounts[1], rowCounts[26], rowCounts[6], rowCounts[27]) + stringIndexSize + blobIndexSize
                11 -> 2 + codedIndexSize(rowCounts[4], rowCounts[8], rowCounts[23]) + blobIndexSize
                12 -> codedIndexSize(
                    rowCounts[6], rowCounts[4], rowCounts[1], rowCounts[2], rowCounts[8], rowCounts[9], rowCounts[10],
                    rowCounts[0], rowCounts[14], rowCounts[23], rowCounts[20], rowCounts[17], rowCounts[26], rowCounts[27],
                    rowCounts[32], rowCounts[35], rowCounts[38], rowCounts[39], rowCounts[40], rowCounts[42], rowCounts[44], rowCounts[43],
                ) + codedIndexSize(rowCounts[6], rowCounts[10], rowCounts[0], rowCounts[26], rowCounts[27]) + blobIndexSize
                13 -> codedIndexSize(rowCounts[4], rowCounts[8]) + blobIndexSize
                14 -> 2 + codedIndexSize(rowCounts[2], rowCounts[6], rowCounts[32]) + blobIndexSize
                15 -> 2 + 4 + tableIndexSize(rowCounts[2])
                16 -> 4 + tableIndexSize(rowCounts[4])
                17 -> blobIndexSize
                18 -> tableIndexSize(rowCounts[2]) + tableIndexSize(rowCounts[20])
                19 -> tableIndexSize(rowCounts[20])
                20 -> 2 + stringIndexSize + codedIndexSize(rowCounts[2], rowCounts[1], rowCounts[27])
                21 -> tableIndexSize(rowCounts[2]) + tableIndexSize(rowCounts[23])
                22 -> tableIndexSize(rowCounts[23])
                23 -> 2 + stringIndexSize + blobIndexSize
                24 -> 2 + tableIndexSize(rowCounts[6]) + codedIndexSize(rowCounts[20], rowCounts[23])
                25 -> tableIndexSize(rowCounts[20])
                26 -> stringIndexSize
                27 -> blobIndexSize
                28 -> 2 + codedIndexSize(rowCounts[4], rowCounts[6], rowCounts[2], rowCounts[1], rowCounts[20], rowCounts[23], rowCounts[26], rowCounts[27], rowCounts[32], rowCounts[35], rowCounts[38], rowCounts[39], rowCounts[40], rowCounts[42], rowCounts[44], rowCounts[43]) + stringIndexSize + tableIndexSize(rowCounts[26])
                29 -> 4 + tableIndexSize(rowCounts[4])
                32 -> 4 + 4 + stringIndexSize + blobIndexSize + tableIndexSize(rowCounts[8])
                33 -> 4
                34 -> 4 + 4 + 4
                35 -> 2 + 2 + 2 + 2 + 4 + blobIndexSize + stringIndexSize + stringIndexSize + tableIndexSize(rowCounts[0])
                36 -> 4 + tableIndexSize(rowCounts[35])
                37 -> 4 + 4 + 4 + tableIndexSize(rowCounts[35])
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
            require(rowCounts.isNotEmpty()) { "codedIndexSize requires at least one table" }
            val tagBits = ceilLog2(rowCounts.size)
            val maxRowCount = rowCounts.maxOrNull() ?: 0
            return if (maxRowCount < (1 shl (16 - tagBits))) 2 else 4
        }

        private fun ceilLog2(value: Int): Int {
            var bits = 0
            var current = 1
            while (current < value) {
                current = current shl 1
                bits++
            }
            return bits
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

        private fun readBlobIndex(
            tablesHeap: ByteBuffer,
            blobHeap: ByteBuffer,
            offset: Int,
            size: Int,
        ): ByteArray {
            val index = readIndex(tablesHeap, offset, size)
            if (index <= 0 || index >= blobHeap.limit()) {
                return byteArrayOf()
            }
            val reader = BlobReader(readRemainingBytes(blobHeap, index))
            val length = reader.readCompressedUInt()
            val payloadOffset = index + blobLengthPrefixSize(blobHeap, index)
            if (payloadOffset < 0 || payloadOffset > blobHeap.limit()) {
                return byteArrayOf()
            }
            val availableLength = blobHeap.limit() - payloadOffset
            if (length < 0 || length > availableLength) {
                return byteArrayOf()
            }
            return readRemainingBytes(blobHeap, payloadOffset, length)
        }

        private fun blobLengthPrefixSize(blobHeap: ByteBuffer, index: Int): Int {
            val first = blobHeap.get(index).toInt() and 0xFF
            return when {
                first and 0x80 == 0 -> 1
                first and 0xC0 == 0x80 -> 2
                else -> 4
            }
        }

        private fun readRemainingBytes(blobHeap: ByteBuffer, offset: Int, length: Int = blobHeap.limit() - offset): ByteArray {
            if (offset < 0 || offset > blobHeap.limit() || length < 0 || offset + length > blobHeap.limit()) {
                return byteArrayOf()
            }
            val bytes = ByteArray(length)
            val duplicate = blobHeap.duplicate()
            duplicate.position(offset)
            duplicate.get(bytes)
            return bytes
        }

        private fun readIndex(buffer: ByteBuffer, offset: Int, size: Int): Int {
            return if (size == 2) {
                buffer.getShort(offset).toInt() and 0xFFFF
            } else {
                val value = buffer.getInt(offset).toLong() and 0xFFFF_FFFFL
                require(value <= Int.MAX_VALUE.toLong()) {
                    "Metadata index exceeds Int range: $value"
                }
                value.toInt()
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

        private fun readUInt16(buffer: ByteBuffer, offset: Int): Int = buffer.getShort(offset).toInt() and 0xFFFF

        private fun align4(value: Int): Int = (value + 3) and -4
    }
}

private data class ParsedMethodSignature(
    val returnType: String,
    val parameterTypes: List<String>,
)
