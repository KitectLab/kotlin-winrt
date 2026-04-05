package dev.winrt.winmd.plugin

internal data class MetadataTables(
    val typeRefs: List<TypeReferenceRow>,
    val typeDefs: List<TypeDefRow>,
    val fieldRows: List<FieldRow>,
    val typeSpecRows: List<TypeSpecRow>,
    val interfaceImplRows: List<InterfaceImplRow>,
    val memberRefRows: List<MemberRefRow>,
    val constantRows: List<ConstantRow>,
    val customAttributeRows: List<CustomAttributeRow>,
    val methodDefs: List<MethodDefRow>,
    val paramRows: List<ParamRow>,
    val propertyMapRows: List<PropertyMapRow>,
    val propertyRows: List<PropertyRow>,
    val methodSemanticsRows: List<MethodSemanticsRow>,
    val genericParamRows: List<GenericParamRow>,
)
