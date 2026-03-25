package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModelFactory
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

fun main(args: Array<String>) {
    val inputs = WinMdParserInputResolver.resolve(args)
    val outputDir = inputs.outputDir
    val sources = inputs.sources

    val model = WinMdModelFactory.minimalModel(sources)
    val generatedFiles = KotlinBindingGenerator().generate(model)

    generatedFiles.forEach { file ->
        val target = outputDir.resolve(file.relativePath)
        target.parent.createDirectories()
        target.writeText(file.content)
    }

    val manifest = outputDir.resolve("manifest.txt")
    manifest.writeText(
        buildString {
            appendLine("Generated ${generatedFiles.size} binding files")
            appendLine("Source WinMD files:")
            sources.forEach { appendLine(it.toString()) }
            appendLine("Generated files:")
            generatedFiles.forEach { appendLine(it.relativePath) }
        },
    )

    if (!Files.exists(outputDir)) {
        error("Output directory was not created: $outputDir")
    }
}
