package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asTypeName
import dev.winrt.winmd.plugin.WinMdMethod

internal sealed interface DelegateLambdaPlan {
    val lambdaType: LambdaTypeName

    data class DirectBridge(
        override val lambdaType: LambdaTypeName,
        val bridgeFactoryMethod: String,
    ) : DelegateLambdaPlan

    data class ObjectBridge(
        override val lambdaType: LambdaTypeName,
        val bridgeFactoryMethod: String,
        val callbackArgType: TypeName,
    ) : DelegateLambdaPlan
}

internal class DelegateLambdaPlanResolver(
    private val typeNameMapper: TypeNameMapper,
) {
    fun resolve(
        invokeMethod: WinMdMethod,
        currentNamespace: String,
        genericParameters: Set<String> = emptySet(),
        supportsObjectType: (String) -> Boolean,
    ): DelegateLambdaPlan? {
        val returnType = kotlinScalarType(invokeMethod.returnType, currentNamespace, genericParameters)
        if (returnType == null && invokeMethod.returnType !in setOf("Unit", "Boolean")) {
            return null
        }

        return when {
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Unit" -> {
                DelegateLambdaPlan.DirectBridge(
                    lambdaType = LambdaTypeName.get(returnType = Unit::class.asTypeName()),
                    bridgeFactoryMethod = "createNoArgUnitDelegate",
                )
            }
            invokeMethod.parameters.isEmpty() && invokeMethod.returnType == "Boolean" -> {
                DelegateLambdaPlan.DirectBridge(
                    lambdaType = LambdaTypeName.get(returnType = Boolean::class.asTypeName()),
                    bridgeFactoryMethod = "createNoArgBooleanDelegate",
                )
            }
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int32" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(Int::class.asTypeName(), Unit::class.asTypeName(), "createInt32ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int32" &&
                invokeMethod.returnType == "Boolean" -> directSingleArgPlan(Int::class.asTypeName(), Boolean::class.asTypeName(), "createInt32ArgBooleanDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "String" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(String::class.asTypeName(), Unit::class.asTypeName(), "createStringArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "String" &&
                invokeMethod.returnType == "Boolean" -> directSingleArgPlan(String::class.asTypeName(), Boolean::class.asTypeName(), "createStringArgBooleanDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "UInt32" &&
                invokeMethod.returnType == "Boolean" -> directSingleArgPlan(UInt::class.asTypeName(), Boolean::class.asTypeName(), "createUInt32ArgBooleanDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "UInt32" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(UInt::class.asTypeName(), Unit::class.asTypeName(), "createUInt32ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int64" &&
                invokeMethod.returnType == "Boolean" -> directSingleArgPlan(Long::class.asTypeName(), Boolean::class.asTypeName(), "createInt64ArgBooleanDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "UInt64" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(ULong::class.asTypeName(), Unit::class.asTypeName(), "createUInt64ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Int64" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(Long::class.asTypeName(), Unit::class.asTypeName(), "createInt64ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Boolean" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(Boolean::class.asTypeName(), Unit::class.asTypeName(), "createBooleanArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Float32" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(Float::class.asTypeName(), Unit::class.asTypeName(), "createFloat32ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                invokeMethod.parameters.single().type == "Float64" &&
                invokeMethod.returnType == "Unit" -> directSingleArgPlan(Double::class.asTypeName(), Unit::class.asTypeName(), "createFloat64ArgUnitDelegate")
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Unit" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.ObjectBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Unit::class.asTypeName()),
                    bridgeFactoryMethod = "createObjectArgUnitDelegate",
                    callbackArgType = callbackArgType,
                )
            }
            invokeMethod.parameters.size == 1 &&
                supportsObjectType(invokeMethod.parameters.single().type) &&
                invokeMethod.returnType == "Boolean" -> {
                val callbackArgType = typeNameMapper.mapTypeName(invokeMethod.parameters.single().type, currentNamespace, genericParameters)
                DelegateLambdaPlan.ObjectBridge(
                    lambdaType = LambdaTypeName.get(parameters = arrayOf(callbackArgType), returnType = Boolean::class.asTypeName()),
                    bridgeFactoryMethod = "createObjectArgBooleanDelegate",
                    callbackArgType = callbackArgType,
                )
            }
            else -> null
        }
    }

    private fun directSingleArgPlan(parameterType: TypeName, returnType: TypeName, bridgeFactoryMethod: String): DelegateLambdaPlan.DirectBridge {
        return DelegateLambdaPlan.DirectBridge(
            lambdaType = LambdaTypeName.get(parameters = arrayOf(parameterType), returnType = returnType),
            bridgeFactoryMethod = bridgeFactoryMethod,
        )
    }

    private fun kotlinScalarType(typeName: String, currentNamespace: String, genericParameters: Set<String>): TypeName? {
        return when (typeName) {
            "Unit" -> Unit::class.asTypeName()
            "String" -> String::class.asTypeName()
            "Boolean" -> Boolean::class.asTypeName()
            "Int32" -> Int::class.asTypeName()
            "UInt32" -> UInt::class.asTypeName()
            "Int64" -> Long::class.asTypeName()
            "UInt64" -> ULong::class.asTypeName()
            "Float32" -> Float::class.asTypeName()
            "Float64" -> Double::class.asTypeName()
            else -> if (typeName == "Object" || typeName.contains('.')) {
                typeNameMapper.mapTypeName(typeName, currentNamespace, genericParameters)
            } else {
                null
            }
        }
    }
}
