package dev.winrt.winmd.parser

import dev.winrt.winmd.plugin.WinMdModel

class KotlinBindingGenerator {
    fun generate(model: WinMdModel): List<GeneratedFile> {
        val typeFileEmitter = TypeFileEmitter(TypeRegistry(model))
        return model.namespaces.flatMap { namespace ->
            namespace.types.mapNotNull { type -> typeFileEmitter.emit(namespace, type) }
        }
    }
}
