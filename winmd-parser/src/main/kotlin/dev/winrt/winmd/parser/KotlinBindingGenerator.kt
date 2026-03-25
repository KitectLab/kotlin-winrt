package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel

class KotlinBindingGenerator {
    private val typeFileEmitter = TypeFileEmitter()

    fun generate(model: WinMdModel): List<GeneratedFile> {
        return model.namespaces.flatMap { namespace ->
            namespace.types.map { type -> typeFileEmitter.emit(namespace, type) }
        }
    }
}
