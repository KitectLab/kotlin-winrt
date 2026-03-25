package dev.winrt.winmd.plugin

internal data class MetadataTables(
    val typeRefs: List<TypeReferenceRow>,
    val typeDefs: List<TypeDefRow>,
    val typeSpecRows: List<TypeSpecRow>,
    val interfaceImplRows: List<InterfaceImplRow>,
    val memberRefRows: List<MemberRefRow>,
    val customAttributeRows: List<CustomAttributeRow>,
    val methodDefs: List<MethodDefRow>,
    val paramRows: List<ParamRow>,
    val propertyMapRows: List<PropertyMapRow>,
    val propertyRows: List<PropertyRow>,
    val methodSemanticsRows: List<MethodSemanticsRow>,
)
