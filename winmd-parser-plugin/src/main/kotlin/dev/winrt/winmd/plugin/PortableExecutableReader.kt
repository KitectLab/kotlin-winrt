package dev.winrt.winmd.plugin

import java.nio.file.Files
import java.nio.file.Path

data class PortableExecutableInfo(
    val path: Path,
    val size: Long,
    val isPortableExecutable: Boolean,
)

object PortableExecutableReader {
    fun inspect(path: Path): PortableExecutableInfo {
        val bytes = Files.readAllBytes(path)
        val isPe = bytes.size >= 2 && bytes[0] == 'M'.code.toByte() && bytes[1] == 'Z'.code.toByte()
        return PortableExecutableInfo(
            path = path,
            size = bytes.size.toLong(),
            isPortableExecutable = isPe,
        )
    }
}
