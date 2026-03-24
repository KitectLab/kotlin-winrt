package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    require(args.size >= 2) {
        "Usage: winmd-parser <outputDir> <winmdFile> [<winmdFile> ...]"
    }

    val outputDir = Path.of(args.first())
    val sources = args.drop(1).map(Path::of)

    val model = WinMdModelFactory.minimalModel(sources)
    val generatedFiles = KotlinBindingGenerator().generate(model)

    generatedFiles.forEach { file ->
        val target = outputDir.resolve(file.relativePath.lowercase())
        target.parent.createDirectories()
        target.writeText(file.content)
    }

    val manifest = outputDir.resolve("manifest.txt")
    manifest.writeText(
        buildString {
            appendLine("Generated ${generatedFiles.size} binding files")
            generatedFiles.forEach { appendLine(it.relativePath.lowercase()) }
        },
    )

    if (!Files.exists(outputDir)) {
        error("Output directory was not created: $outputDir")
    }
}
