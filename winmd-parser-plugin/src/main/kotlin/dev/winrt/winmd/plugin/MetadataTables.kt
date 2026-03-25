package dev.winrt.winmd.plugin

internal data class MetadataTables(
    val typeRefs: List<TypeReferenceRow>,
    val typeDefs: List<TypeDefRow>,
    val methodDefs: List<MethodDefRow>,
    val paramRows: List<ParamRow>,
)
