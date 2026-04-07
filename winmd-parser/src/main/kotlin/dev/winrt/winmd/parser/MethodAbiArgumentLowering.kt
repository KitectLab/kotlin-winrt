package dev.winrt.winmd.parser

internal fun abiArgumentExpression(
    argumentName: String,
    parameterType: String,
    category: MethodParameterCategory,
    lowerObjectArgument: () -> Any,
): Any = when (category) {
    MethodParameterCategory.OBJECT -> lowerObjectArgument()
    MethodParameterCategory.INT32,
    MethodParameterCategory.UINT32,
    MethodParameterCategory.BOOLEAN,
    MethodParameterCategory.INT64,
    MethodParameterCategory.EVENT_REGISTRATION_TOKEN -> int64AbiArgumentExpression(argumentName, parameterType)
    MethodParameterCategory.STRING -> argumentName
}

internal fun <T> abiArgumentExpressions(
    parameters: List<T>,
    parameterCategories: List<MethodParameterCategory>,
    argumentName: (T) -> String,
    parameterType: (T) -> String,
    lowerObjectArgument: (T) -> Any,
): List<Any> =
    parameters.zip(parameterCategories) { parameter, category ->
        abiArgumentExpression(
            argumentName = argumentName(parameter),
            parameterType = parameterType(parameter),
            category = category,
            lowerObjectArgument = { lowerObjectArgument(parameter) },
        )
    }
