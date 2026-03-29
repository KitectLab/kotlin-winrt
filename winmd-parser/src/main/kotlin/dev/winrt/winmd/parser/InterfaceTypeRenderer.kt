package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod
import dev.winrt.winmd.plugin.WinMdProperty
import dev.winrt.winmd.plugin.WinMdType

internal class InterfaceTypeRenderer(
    private val typeNameMapper: TypeNameMapper,
    private val delegateLambdaPlanResolver: DelegateLambdaPlanResolver,
    private val typeRegistry: TypeRegistry,
    private val winRtSignatureMapper: WinRtSignatureMapper,
    private val winRtProjectionTypeMapper: WinRtProjectionTypeMapper,
    private val kotlinCollectionProjectionMapper: KotlinCollectionProjectionMapper = KotlinCollectionProjectionMapper(),
) {
    fun render(type: WinMdType): TypeSpec {
        val rawTypeClass = ClassName(type.namespace.lowercase(), type.name)
        val typeVariables = type.genericParameters.map { TypeVariableName(it) }
        val typeClass = if (typeVariables.isEmpty()) rawTypeClass else rawTypeClass.parameterizedBy(typeVariables)
        val genericParameters = type.genericParameters.toSet()
        return TypeSpec.classBuilder(type.name)
            .addModifiers(KModifier.OPEN)
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .primaryConstructor(pointerConstructor())
            .superclass(PoetSymbols.winRtInterfaceProjectionClass)
            .addSuperclassConstructorParameter("pointer")
            .apply {
                type.baseInterfaces.mapNotNull { baseInterface ->
                    collectionSuperinterface(baseInterface, type.namespace, genericParameters)
                }.forEach { addSuperinterface(it) }
                kotlinCollectionProjectionMapper.interfaceProjection(type)?.let { projection ->
                    addSuperinterface(projection.superinterface)
                    projection.extraProperties.forEach(::addProperty)
                    projection.extraFunctions.forEach(::addFunction)
                    addProperty(kotlinCollectionProjectionMapper.buildWinRtSizeProperty(projection.winRtSizeSlot))
                }
                if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableIterable") {
                    addSuperinterface(PoetSymbols.iterableClass.parameterizedBy(PoetSymbols.inspectableClass))
                    addFunction(
                        FunSpec.builder("iterator")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass))
                            .addStatement("return first()")
                            .build(),
                    )
                }
                if (type.namespace == "Microsoft.UI.Xaml.Interop" && type.name == "IBindableIterator") {
                    addSuperinterface(PoetSymbols.iteratorClass.parameterizedBy(PoetSymbols.inspectableClass))
                    addProperty(
                        PropertySpec.builder("winRtCurrent", PoetSymbols.inspectableClass)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement(
                                        "return %T(%T.invokeObjectMethod(pointer, 6).getOrThrow())",
                                        PoetSymbols.inspectableClass,
                                        PoetSymbols.platformComInteropClass,
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    addProperty(
                        PropertySpec.builder("winRtHasCurrent", PoetSymbols.winRtBooleanClass)
                            .getter(
                                FunSpec.getterBuilder()
                                    .addStatement(
                                        "return %T(%T.invokeBooleanGetter(pointer, 7).getOrThrow())",
                                        PoetSymbols.winRtBooleanClass,
                                        PoetSymbols.platformComInteropClass,
                                    )
                                    .build(),
                            )
                            .build(),
                    )
                    addFunction(
                        FunSpec.builder("hasNext")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(Boolean::class)
                            .addStatement("return winRtHasCurrent.value")
                            .build(),
                    )
                    addFunction(
                        FunSpec.builder("next")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(PoetSymbols.inspectableClass)
                            .beginControlFlow("if (!hasNext())")
                            .addStatement("throw %T()", NoSuchElementException::class)
                            .endControlFlow()
                            .addStatement("val current = winRtCurrent")
                            .addStatement("moveNext()")
                            .addStatement("return current")
                            .build(),
                    )
                }
            }
            .addProperties(type.properties.mapNotNull { renderProperty(it, type.namespace, genericParameters) })
            .addFunctions(type.methods.mapNotNull { renderMethod(it, type.namespace, genericParameters) })
            .addFunctions(type.methods.mapNotNull { renderLambdaOverload(it, type.namespace, genericParameters) })
            .addType(
                TypeSpec.companionObjectBuilder()
                    .addSuperinterface(PoetSymbols.winRtInterfaceMetadataClass)
                    .addProperty(overrideStringProperty("qualifiedName", "${type.namespace}.${type.name}"))
                    .addProperty(
                        overrideStringProperty(
                            "projectionTypeKey",
                            winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace),
                        ),
                    )
                    .addProperty(
                        PropertySpec.builder("iid", PoetSymbols.guidClass)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("%M(%S)", PoetSymbols.guidOfMember, type.guid ?: "00000000-0000-0000-0000-000000000000")
                            .build(),
                    )
                    .apply {
                        if (type.genericParameters.isEmpty()) {
                            addFunction(
                                FunSpec.builder("from")
                                    .apply {
                                        typeVariables.forEach(::addTypeVariable)
                                    }
                                    .returns(typeClass)
                                    .addParameter("inspectable", PoetSymbols.inspectableClass)
                                    .addStatement("return inspectable.%M(this, ::%L)", PoetSymbols.projectInterfaceMember, type.name)
                                    .build(),
                            )
                        } else {
                            addFunction(renderGenericSignatureOf(type))
                            addFunction(renderGenericProjectionTypeKeyOf(type))
                            addFunction(renderGenericIidOf())
                            addFunction(renderGenericMetadataOf(type))
                            addFunction(renderGenericFrom(type, typeClass, typeVariables))
                        }
                    }
                    .build(),
            )
            .build()
    }

    private fun renderGenericSignatureOf(type: WinMdType): FunSpec {
        val argumentSignatureVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}Signature" }
        return FunSpec.builder("signatureOf")
            .returns(String::class)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}Signature", String::class).build()
                },
            )
            .addStatement(
                "return %T.parameterizedInterface(%S, $argumentSignatureVars)",
                PoetSymbols.winRtTypeSignatureClass,
                type.guid ?: "00000000-0000-0000-0000-000000000000",
            )
            .build()
    }

    private fun renderGenericIidOf(): FunSpec {
        return FunSpec.builder("iidOf")
            .returns(PoetSymbols.guidClass)
            .addParameter(
                ParameterSpec.builder("argumentSignatures", String::class)
                    .addModifiers(KModifier.VARARG)
                    .build(),
            )
            .addStatement("return %T.createFromSignature(signatureOf(*argumentSignatures))", PoetSymbols.parameterizedInterfaceIdClass)
            .build()
    }

    private fun renderGenericProjectionTypeKeyOf(type: WinMdType): FunSpec {
        val argumentTypeKeyVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}ProjectionTypeKey" }
        val mappedRawType = winRtProjectionTypeMapper.projectionTypeKeyFor("${type.namespace}.${type.name}", type.namespace)
        return FunSpec.builder("projectionTypeKeyOf")
            .returns(String::class)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
                },
            )
            .addStatement(
                "return %S + %S + listOf($argumentTypeKeyVars).joinToString(%S) + %S",
                "$mappedRawType<",
                "",
                ", ",
                ">",
            )
            .build()
    }

    private fun renderGenericMetadataOf(type: WinMdType): FunSpec {
        val metadataType = PoetSymbols.winRtInterfaceMetadataClass
        val argumentSignatureVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}Signature" }
        val argumentTypeKeyVars = type.genericParameters.indices.joinToString(", ") { index -> "arg${index}ProjectionTypeKey" }
        return FunSpec.builder("metadataOf")
            .returns(metadataType)
            .addParameters(
                type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}Signature", String::class).build()
                } + type.genericParameters.indices.map { index ->
                    ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
                }
            )
            .addCode(
                CodeBlock.builder()
                    .add("return object : %T {\n", metadataType)
                    .indent()
                    .add("override val qualifiedName: String = %S\n", "${type.namespace}.${type.name}")
                    .add("override val projectionTypeKey: String = projectionTypeKeyOf($argumentTypeKeyVars)\n")
                    .add("override val iid: %T = iidOf($argumentSignatureVars)\n", PoetSymbols.guidClass)
                    .unindent()
                    .add("}\n")
                    .build(),
            )
            .build()
    }

    private fun renderGenericFrom(
        type: WinMdType,
        typeClass: TypeName,
        typeVariables: List<TypeVariableName>,
    ): FunSpec {
        val argumentParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}Signature", String::class).build()
        }
        val projectionTypeKeyParameters = type.genericParameters.indices.map { index ->
            ParameterSpec.builder("arg${index}ProjectionTypeKey", String::class).build()
        }
        val metadataArgumentNames = (argumentParameters + projectionTypeKeyParameters).joinToString(", ") { it.name }
        return FunSpec.builder("from")
            .apply {
                typeVariables.forEach(::addTypeVariable)
            }
            .returns(typeClass)
            .addParameter("inspectable", PoetSymbols.inspectableClass)
            .addParameters(argumentParameters)
            .addParameters(projectionTypeKeyParameters)
            .addStatement(
                "return inspectable.%M(metadataOf($metadataArgumentNames), ::%L)",
                PoetSymbols.projectInterfaceMember,
                type.name,
            )
            .build()
    }

    private fun renderProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PropertySpec? {
        if (!supportsInterfaceProperty(property, currentNamespace, genericParameters)) {
            return null
        }
        val propertyName = property.name.replaceFirstChar(Char::lowercase)
        val propertyType = typeNameMapper.mapTypeName(property.type, currentNamespace, genericParameters)
        val getterVtableIndex = property.getterVtableIndex!!
        val getterBuilder = FunSpec.getterBuilder()
        when (PropertyRuleRegistry.interfaceGetterRuleFamily(property.type, typeRegistry.isEnumType(property.type, currentNamespace))) {
            InterfacePropertyRuleFamily.ENUM ->
                getterBuilder.addStatement(
                    "return %T.fromValue(%T.invokeUInt32Method(pointer, %L).getOrThrow().toInt())",
                    propertyType,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            InterfacePropertyRuleFamily.INT32 ->
                getterBuilder.addStatement(
                    "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                    PoetSymbols.int32Class,
                    PoetSymbols.platformComInteropClass,
                    getterVtableIndex,
                )
            else -> return null
        }
        val propertyBuilder = PropertySpec.builder(propertyName, propertyType)
            .getter(getterBuilder.build())
        if (property.mutable &&
            PropertyRuleRegistry.interfaceSetterRuleFamily(property.type) == InterfacePropertyRuleFamily.INT32 &&
            property.setterVtableIndex != null
        ) {
            val setterVtableIndex = property.setterVtableIndex!!
            propertyBuilder.mutable()
            propertyBuilder.setter(
                FunSpec.setterBuilder()
                    .addParameter("value", propertyType)
                    .addStatement(
                        "%T.invokeInt32Setter(pointer, %L, value.value).getOrThrow()",
                        PoetSymbols.platformComInteropClass,
                        setterVtableIndex,
                    )
                    .build(),
            )
        }
        return propertyBuilder.build()
    }

    private fun renderMethod(method: WinMdMethod, currentNamespace: String, genericParameters: Set<String>): FunSpec? {
        if (!isKotlinIdentifier(method.name)) {
            return null
        }
        if (!supportsInterfaceMethod(method, currentNamespace, genericParameters)) {
            return null
        }
        val functionName = kotlinMethodName(method.name)
        val builder = FunSpec.builder(functionName)
            .apply {
                if (method.name == "ToString" && method.returnType == "String" && method.parameters.isEmpty()) {
                    addModifiers(KModifier.OVERRIDE)
                }
            }
            .returns(typeNameMapper.mapTypeName(method.returnType, currentNamespace, genericParameters))
            .addParameters(method.parameters.map { parameter ->
                ParameterSpec.builder(
                    parameter.name.replaceFirstChar(Char::lowercase),
                    typeNameMapper.mapTypeName(parameter.type, currentNamespace, genericParameters),
                ).build()
            })
        val methodPlan = plannedInterfaceMethod(method, currentNamespace, genericParameters)
        if (methodPlan != null) {
            return builder
                .addStatement(methodPlan.statement, *methodPlan.args(method, currentNamespace))
                .build()
        }

        return when {
            method.returnType == "String" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement("return %L", HStringSupport.toKotlinString("pointer", vtableIndex))
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement("return %L", HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithString(vtableIndex, argumentName)))
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement("return %L", HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithInt32(vtableIndex, "$argumentName.value")))
                    .build()
            }
            method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement("return %L", HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithUInt32(vtableIndex, "$argumentName.value")))
                    .build()
            }
            method.returnType == "Float64" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64Method(pointer, %L).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Float64" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeFloat64MethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.float64Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanGetter(pointer, %L).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type) &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithObjectArg(pointer, %L, %N.pointer).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Boolean" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeBooleanMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        PoetSymbols.winRtBooleanClass,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            method.returnType == "Int32" && method.parameters.isEmpty() && method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                builder
                    .addStatement(
                        "return %T(%T.invokeInt32Method(pointer, %L).getOrThrow())",
                        PoetSymbols.int32Class,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T.fromValue(%T.invokeUInt32Method(pointer, %L).getOrThrow().toInt())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null -> {
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethod(pointer, %L).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethodWithStringArg(pointer, %L, %N).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            supportsInterfaceObjectType(method.returnType) &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "UInt32" &&
                method.vtableIndex != null -> {
                val argumentName = method.parameters[0].name.replaceFirstChar(Char::lowercase)
                val vtableIndex = method.vtableIndex!!
                val returnType = typeNameMapper.mapTypeName(method.returnType, currentNamespace)
                builder
                    .addStatement(
                        "return %T(%T.invokeObjectMethodWithUInt32Arg(pointer, %L, %N.value).getOrThrow())",
                        returnType,
                        PoetSymbols.platformComInteropClass,
                        vtableIndex,
                        argumentName,
                    )
                    .build()
            }
            else -> null
        }
    }

    private fun renderLambdaOverload(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): FunSpec? {
        if (method.parameters.size != 1 || method.vtableIndex == null) {
            return null
        }
        val delegateType = typeRegistry.findType(method.parameters.single().type, currentNamespace) ?: return null
        if (delegateType.kind != dev.winrt.winmd.plugin.WinMdTypeKind.Delegate) {
            return null
        }
        val invokeMethod = delegateType.methods.singleOrNull { it.name == "Invoke" } ?: return null
        if (invokeMethod.returnType != "Unit" && invokeMethod.returnType != "Boolean") {
            return null
        }

        val functionName = kotlinMethodName(method.name)
        val delegateClass = typeNameMapper.mapTypeName(method.parameters.single().type, currentNamespace, genericParameters)
        val lambdaParameterName = "callback"
        val plan = delegateLambdaPlanResolver.resolve(
            invokeMethod = invokeMethod,
            currentNamespace = currentNamespace,
            genericParameters = genericParameters,
            supportsObjectType = ::supportsInterfaceObjectType,
        ) as? DelegateLambdaPlan.PlannedBridge ?: return null
        return FunSpec.builder(functionName)
            .returns(PoetSymbols.winRtDelegateHandleClass)
            .addParameter(lambdaParameterName, plan.lambdaType)
            .apply {
                val argumentKindsLiteral = if (plan.bridge.argumentKinds.isEmpty()) {
                    "emptyList()"
                } else {
                    plan.bridge.argumentKinds.joinToString(
                        prefix = "listOf(",
                        postfix = ")",
                    ) { kind -> "${PoetSymbols.winRtDelegateValueKindClass.canonicalName}.${kind.name}" }
                }
                val callbackInvocation = buildDelegateCallbackInvocation(plan, lambdaParameterName)
                addStatement(
                    "val delegateHandle = %T.%L(%T.iid, %L) { args -> %L }",
                    PoetSymbols.winRtDelegateBridgeClass,
                    plan.bridge.factoryMethod,
                    delegateClass,
                    argumentKindsLiteral,
                    callbackInvocation,
                )
                addStatement("%N(%T(delegateHandle.pointer))", functionName, delegateClass)
                addStatement("return delegateHandle")
            }
            .build()
    }

    private fun buildDelegateCallbackInvocation(
        plan: DelegateLambdaPlan.PlannedBridge,
        callbackName: String,
    ): CodeBlock {
        if (plan.lambdaType.parameters.isEmpty()) {
            return CodeBlock.of("%N()", callbackName)
        }
        val builder = CodeBlock.builder().add("%N(", callbackName)
        plan.lambdaType.parameters.indices.forEachIndexed { position, index ->
            if (position > 0) {
                builder.add(", ")
            }
            val parameterType = plan.lambdaType.parameters[index]
            when (plan.bridge.argumentKinds[index]) {
                DelegateArgumentKind.OBJECT -> builder.add("%L(args[%L] as %T)", parameterType, index, PoetSymbols.comPtrClass)
                else -> builder.add("args[%L] as %L", index, parameterType)
            }
        }
        return builder.add(")").build()
    }

    private fun kotlinMethodName(methodName: String): String {
        return when (methodName) {
            "ToString" -> "toString"
            else -> methodName.replaceFirstChar(Char::lowercase)
        }
    }

    private fun supportsInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return plannedInterfaceMethod(method, currentNamespace, genericParameters) != null ||
            (method.returnType == "String" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Int32" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (typeRegistry.isEnumType(method.returnType, currentNamespace) &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.isEmpty() &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "String" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                method.parameters[0].type == "Int32" &&
                method.vtableIndex != null) ||
            (method.returnType == "Unit" &&
                method.parameters.size == 1 &&
                supportsInterfaceObjectInput(method.parameters[0].type) &&
                method.vtableIndex != null)
    }

    private fun plannedInterfaceMethod(
        method: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        if (method.vtableIndex == null) {
            return null
        }
        val signatureKey = methodSignatureKey(
            returnType = method.returnType,
            parameterTypes = method.parameters.map { it.type },
            supportsObjectType = ::supportsInterfaceObjectInput,
        )
        return signatureKey
            ?.takeIf { MethodRuleRegistry.sharedMethodRuleFamily(it) != null }
            ?.let { plannedInterfaceMethodForKey(it, genericParameters) }
    }

    private fun plannedInterfaceMethodForKey(
        signatureKey: MethodSignatureKey,
        genericParameters: Set<String>,
    ): PlannedInterfaceMethod? {
        return when (signatureKey) {
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethod(method.vtableIndex!!)))
                },
            )
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithString(method.vtableIndex!!, argumentName)))
                },
            )
            MethodSignatureKey(MethodReturnKind.STRING, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(HStringSupport.fromCall(AbiCallCatalog.hstringMethodWithUInt32(method.vtableIndex!!, "$argumentName.value")))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64Method(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64MethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.FLOAT64, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.float64Class, AbiCallCatalog.float64MethodWithUInt32(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithString(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, AbiCallCatalog.booleanMethodWithUInt32(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.BOOLEAN, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "return %T(%T.invokeBooleanMethodWithObjectArg(pointer, %L, %N.pointer).getOrThrow())",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.winRtBooleanClass, PoetSymbols.platformComInteropClass, method.vtableIndex!!, argumentName)
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, namespace, genericParameters),
                        AbiCallCatalog.objectMethod(method.vtableIndex!!),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, namespace, genericParameters),
                        AbiCallCatalog.objectMethodWithString(method.vtableIndex!!, argumentName),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.OBJECT, MethodSignatureShape.UINT32) -> PlannedInterfaceMethod(
                statement = "return %T(%L)",
                args = { method, namespace ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(
                        typeNameMapper.mapTypeName(method.returnType, namespace, genericParameters),
                        AbiCallCatalog.objectMethodWithUInt32(method.vtableIndex!!, argumentName),
                    )
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.EMPTY) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    arrayOf(AbiCallCatalog.unitMethod(method.vtableIndex!!))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.STRING) -> PlannedInterfaceMethod(
                statement = "%T.invokeStringSetter(pointer, %L, %N).getOrThrow()",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(PoetSymbols.platformComInteropClass, method.vtableIndex!!, argumentName)
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.INT32) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.unitMethodWithInt32(method.vtableIndex!!, argumentName))
                },
            )
            MethodSignatureKey(MethodReturnKind.UNIT, MethodSignatureShape.OBJECT) -> PlannedInterfaceMethod(
                statement = "%L",
                args = { method, _ ->
                    val argumentName = method.parameters.single().name.replaceFirstChar(Char::lowercase)
                    arrayOf(AbiCallCatalog.objectSetter(method.vtableIndex!!, argumentName))
                },
            )
            else -> null
        }
    }

    private data class PlannedInterfaceMethod(
        val statement: String,
        val args: (WinMdMethod, String) -> Array<Any>,
    )

    private fun supportsInterfaceObjectInput(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }

    private fun supportsInterfaceObjectType(type: String): Boolean {
        return (type == "Object" || type.contains('.')) &&
            !type.contains('`') &&
            !type.contains('<') &&
            !type.endsWith("[]")
    }

    private fun supportsInterfaceProperty(
        property: WinMdProperty,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): Boolean {
        return property.getterVtableIndex != null &&
            PropertyRuleRegistry.interfaceGetterRuleFamily(
                type = property.type,
                isEnumType = typeRegistry.isEnumType(property.type, currentNamespace),
            ) != null
    }

    private fun collectionSuperinterface(
        baseInterface: String,
        currentNamespace: String,
        genericParameters: Set<String>,
    ): TypeName? {
        val mapped = typeNameMapper.mapTypeName(baseInterface, currentNamespace, genericParameters)
        return if (mapped.toString().startsWith("kotlin.collections.")) mapped else null
    }

}
