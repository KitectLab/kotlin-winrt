package dev.winrt.sample.jvm

import dev.winrt.kom.KomException
import dev.winrt.kom.PlatformRuntime
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

object WindowsActivationContext {
    private const val appxNamespace = "http://schemas.microsoft.com/appx/manifest/foundation/windows10"
    private const val envVarName = "MICROSOFT_WINDOWSAPPRUNTIME_BASE_DIRECTORY"
    private val linker: Linker = Linker.nativeLinker()
    private val libraryArena: Arena = Arena.ofAuto()
    private val kernel32: SymbolLookup = SymbolLookup.libraryLookup("kernel32", libraryArena)
    private val actCtxLayout: MemoryLayout = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("cbSize"),
        ValueLayout.JAVA_INT.withName("dwFlags"),
        ValueLayout.ADDRESS.withName("lpSource"),
        ValueLayout.JAVA_SHORT.withName("wProcessorArchitecture"),
        ValueLayout.JAVA_SHORT.withName("wLangId"),
        MemoryLayout.paddingLayout(4),
        ValueLayout.ADDRESS.withName("lpAssemblyDirectory"),
        ValueLayout.ADDRESS.withName("lpResourceName"),
        ValueLayout.ADDRESS.withName("lpApplicationName"),
        ValueLayout.ADDRESS.withName("hModule"),
    )

    class ActivationContext internal constructor(
        private val handle: MemorySegment,
        private val cookie: MemorySegment,
        val manifestPath: Path,
    ) : AutoCloseable {
        override fun close() {
            deactivate(this)
        }

        internal fun handle(): MemorySegment = handle

        internal fun cookie(): MemorySegment = cookie
    }

    fun activateConfigured(root: Path): Result<ActivationContext?> {
        if (!PlatformRuntime.isWindows) {
            return Result.success(null)
        }

        return runCatching {
            val fragmentPaths = configuredAppxFragments()
            if (fragmentPaths.isEmpty()) {
                return@runCatching null
            }
            val frameworkDllNames = configuredFrameworkDllNames(root)

            val manifestPath = root.resolve("WindowsAppSDK-SelfContained.manifest")
            Files.createDirectories(root)
            Files.writeString(manifestPath, buildManifest(fragmentPaths, frameworkDllNames))

            setEnvironmentVariable(
                envVarName,
                root.toAbsolutePath().toString().let { path ->
                    if (path.endsWith("\\") || path.endsWith("/")) path else "$path\\"
                },
            )

            Arena.ofConfined().use { arena ->
                val manifestSegment = allocateWideString(arena, manifestPath.toAbsolutePath().toString())
                val actCtx = arena.allocate(actCtxLayout)
                actCtx.set(ValueLayout.JAVA_INT, 0L, actCtxLayout.byteSize().toInt())
                actCtx.set(ValueLayout.JAVA_INT, 4L, 0)
                actCtx.set(ValueLayout.ADDRESS, 8L, manifestSegment)
                actCtx.set(ValueLayout.JAVA_SHORT, 16L, 0)
                actCtx.set(ValueLayout.JAVA_SHORT, 18L, 0)
                actCtx.set(ValueLayout.ADDRESS, 24L, MemorySegment.NULL)
                actCtx.set(ValueLayout.ADDRESS, 32L, MemorySegment.NULL)
                actCtx.set(ValueLayout.ADDRESS, 40L, MemorySegment.NULL)
                actCtx.set(ValueLayout.ADDRESS, 48L, MemorySegment.NULL)

                val createActCtx = downcall(
                    "CreateActCtxW",
                    FunctionDescriptor.of(
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                    ),
                )
                val handle = createActCtx.invokeWithArguments(actCtx) as MemorySegment
                if (handle.address() == -1L) {
                    throw KomException(
                        "CreateActCtxW failed with GetLastError=${getLastError()} for $manifestPath",
                    )
                }

                val cookieResult = arena.allocate(ValueLayout.ADDRESS)
                val activateActCtx = downcall(
                    "ActivateActCtx",
                    FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS,
                    ),
                )
                val activated = activateActCtx.invokeWithArguments(
                    handle,
                    cookieResult,
                ) as Int
                if (activated == 0) {
                    releaseActCtx(handle)
                    throw KomException(
                        "ActivateActCtx failed with GetLastError=${getLastError()} for $manifestPath",
                    )
                }

                ActivationContext(
                    handle = handle,
                    cookie = cookieResult.get(ValueLayout.ADDRESS, 0L),
                    manifestPath = manifestPath,
                )
            }
        }
    }

    fun deactivate(context: ActivationContext) {
        if (!PlatformRuntime.isWindows) {
            return
        }

        runCatching {
            val deactivateActCtx = downcall(
                "DeactivateActCtx",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                ),
            )
            deactivateActCtx.invokeWithArguments(
                0,
                context.cookie(),
            )
        }
        releaseActCtx(context.handle())
    }

    private fun configuredAppxFragments(): List<Path> {
        val raw = System.getProperty("dev.winrt.windowsAppSdkAppxFragments").orEmpty()
        if (raw.isBlank()) {
            return emptyList()
        }
        return raw.split(java.io.File.pathSeparatorChar)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(Path::of)
            .filter(Files::isRegularFile)
            .distinct()
    }

    private fun configuredFrameworkDllNames(root: Path): List<String> {
        if (!Files.isDirectory(root)) {
            return emptyList()
        }
        return Files.walk(root).use { paths ->
            paths
                .filter(Files::isRegularFile)
                .map { it.fileName.toString() }
                .filter { it.endsWith(".dll", ignoreCase = true) }
                .distinct()
                .sorted()
                .toList()
        }
    }

    private fun buildManifest(fragmentPaths: List<Path>, frameworkDllNames: List<String>): String {
        val documentBuilder = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder()
        val entryByDllName = linkedMapOf<String, String>()
        val remainingDllNames = linkedMapOf<String, String>()
        frameworkDllNames.forEach { dllName ->
            remainingDllNames.putIfAbsent(dllName.lowercase(), dllName)
        }

        fragmentPaths.forEach { fragmentPath ->
            val document = documentBuilder.parse(fragmentPath.toFile())
            val inProcessServers = document.getElementsByTagNameNS(appxNamespace, "InProcessServer")
            for (index in 0 until inProcessServers.length) {
                val server = inProcessServers.item(index)
                if (server !is org.w3c.dom.Element) continue
                val path = firstChildText(server, "Path") ?: continue
                val activatableClasses = server.getElementsByTagNameNS(appxNamespace, "ActivatableClass")
                val body = buildString {
                    for (classIndex in 0 until activatableClasses.length) {
                        val activatableClass = activatableClasses.item(classIndex) as? org.w3c.dom.Element ?: continue
                        val classId = activatableClass.getAttribute("ActivatableClassId").trim()
                        if (classId.isEmpty()) continue
                        append("        <winrtv1:activatableClass name='")
                        append(escapeXml(classId))
                        append("' threadingModel='both'/>\n")
                    }
                }
                if (body.isNotEmpty()) {
                    entryByDllName.putIfAbsent(path.lowercase(), manifestFileEntry(path, body))
                    remainingDllNames.remove(path.lowercase())
                }
            }

            val proxyStubs = document.getElementsByTagNameNS(appxNamespace, "ProxyStub")
            for (index in 0 until proxyStubs.length) {
                val proxyStub = proxyStubs.item(index) as? org.w3c.dom.Element ?: continue
                val path = firstChildText(proxyStub, "Path") ?: continue
                if (path == "PushNotificationsLongRunningTask.ProxyStub.dll" || path == "Microsoft.Windows.Widgets.dll") {
                    continue
                }
                val classId = proxyStub.getAttribute("ClassId").trim()
                val interfaces = proxyStub.getElementsByTagNameNS(appxNamespace, "Interface")
                val body = buildString {
                    if (classId.isNotEmpty()) {
                        append("        <asmv3:comClass clsid='")
                        append(escapeXml(bracedGuid(classId)))
                        append("'/>\n")
                    }
                    for (interfaceIndex in 0 until interfaces.length) {
                        val interfaceElement = interfaces.item(interfaceIndex) as? org.w3c.dom.Element ?: continue
                        val interfaceId = interfaceElement.getAttribute("InterfaceId").trim()
                        val name = interfaceElement.getAttribute("Name").trim()
                        if (interfaceId.isEmpty() || name.isEmpty()) continue
                        append("        <asmv3:comInterfaceProxyStub name='")
                        append(escapeXml(name))
                        append("' iid='")
                        append(escapeXml(bracedGuid(interfaceId)))
                        append("'/>\n")
                    }
                }
                if (body.isNotEmpty()) {
                    entryByDllName.putIfAbsent(path.lowercase(), manifestFileEntry(path, body))
                    remainingDllNames.remove(path.lowercase())
                }
            }
        }

        return buildString {
            appendLine("<?xml version='1.0' encoding='utf-8' standalone='yes'?>")
            appendLine("<assembly manifestVersion='1.0'")
            appendLine("    xmlns:asmv3='urn:schemas-microsoft-com:asm.v3'")
            appendLine("    xmlns:winrtv1='urn:schemas-microsoft-com:winrt.v1'")
            appendLine("    xmlns='urn:schemas-microsoft-com:asm.v1'>")
            appendLine("    <assemblyIdentity type='win32' name='dev.winrt.sample.jvm.windowsappsdk' version='1.0.0.0' processorArchitecture='*'/>")
            entryByDllName.values.forEach(::append)
            remainingDllNames.values.forEach { dllName ->
                append(manifestFileEntry(dllName, ""))
            }
            appendLine("</assembly>")
        }
    }

    private fun manifestFileEntry(path: String, body: String): String {
        return buildString {
            append("    <asmv3:file name='")
            append(escapeXml(path))
            append("' loadFrom='%")
            append(envVarName)
            append("%")
            append(escapeXml(path))
            appendLine("'>")
            append(body)
            appendLine("    </asmv3:file>")
        }
    }

    private fun firstChildText(element: org.w3c.dom.Element, localName: String): String? {
        val children = element.getElementsByTagNameNS(appxNamespace, localName)
        if (children.length == 0) {
            return null
        }
        return children.item(0)?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun escapeXml(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "&apos;")
            .replace("\"", "&quot;")

    private fun setEnvironmentVariable(name: String, value: String) {
        Arena.ofConfined().use { arena ->
            val nameSegment = allocateWideString(arena, name)
            val valueSegment = allocateWideString(arena, value)
            val setEnvironmentVariable = downcall(
                "SetEnvironmentVariableW",
                FunctionDescriptor.of(
                    ValueLayout.JAVA_INT,
                    ValueLayout.ADDRESS,
                    ValueLayout.ADDRESS,
                ),
            )
            val result = setEnvironmentVariable.invokeWithArguments(
                nameSegment,
                valueSegment,
            ) as Int
            if (result == 0) {
                throw KomException("SetEnvironmentVariableW failed with GetLastError=${getLastError()} for $name")
            }
        }
    }

    private fun getLastError(): Int {
        val getLastError = downcall(
            "GetLastError",
            FunctionDescriptor.of(ValueLayout.JAVA_INT),
        )
        return getLastError.invokeWithArguments() as Int
    }

    private fun releaseActCtx(handle: MemorySegment) {
        val releaseActCtx = downcall(
            "ReleaseActCtx",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
        )
        releaseActCtx.invokeWithArguments(handle)
    }

    private fun allocateWideString(arena: Arena, value: String): MemorySegment {
        val bytes = (value + '\u0000').toByteArray(StandardCharsets.UTF_16LE)
        return arena.allocate(bytes.size.toLong(), 2).copyFrom(MemorySegment.ofArray(bytes))
    }

    private fun bracedGuid(value: String): String {
        val normalized = value.trim().removePrefix("{").removeSuffix("}")
        return "{$normalized}"
    }

    private fun downcall(name: String, descriptor: FunctionDescriptor): MethodHandle {
        val symbol = kernel32.find(name).orElse(null)
        requireNotNull(symbol) {
            "Win32 symbol not found: $name"
        }
        return linker.downcallHandle(symbol, descriptor)
    }
}
