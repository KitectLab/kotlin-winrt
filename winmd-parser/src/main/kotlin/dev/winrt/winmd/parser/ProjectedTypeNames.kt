package dev.winrt.winmd.parser

import com.squareup.kotlinpoet.ClassName

internal fun projectedDeclarationSimpleName(metadataName: String): String = metadataName.substringBefore('`')

internal fun projectedDeclarationClassName(namespace: String, metadataName: String): ClassName =
    ClassName(namespace.lowercase(), projectedDeclarationSimpleName(metadataName))
