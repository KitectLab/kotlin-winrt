# kotlin-winrt

`kotlin-winrt` is a Windows-focused Kotlin workspace for building WinRT and WinUI 3 applications from:

- Kotlin/JVM
- Kotlin/Native `mingwX64`

The repository is organized around a layered runtime and generation pipeline:

- `kom`: low-level COM ABI interop primitives
- `winrt-core`: WinRT object model and shared runtime helpers
- `winmd-parser-plugin`: reads WinMD files and builds an intermediate model
- `winmd-parser`: generates Kotlin bindings from the intermediate model
- `generated-winrt-bindings`: checked-in generated bindings used by samples
- `sample-jvm-winui3`: JVM-first sample application exercising the generated bindings

## Development status

This repository currently provides:

- a multi-module Gradle skeleton
- minimal multiplatform runtime abstractions for COM and WinRT
- a small WinMD inspection and code generation pipeline
- a JVM sample that exercises the generated bindings and emits a Windows launch script scaffold
- a JVM interop baseline built around JDK 22+ Foreign Function and Memory API

The native JVM and WinUI 3 runtime bridge is intentionally kept behind interfaces so the project can evolve without rewriting generated bindings.

## Build

Use Windows for full build and runtime validation. JVM builds target JDK 22 or newer because the JVM-side Win32/COM bridge is based on the stable FFM API in `java.lang.foreign`.

### Linux/macOS static verification

```bash
./gradlew test
```

### Windows validation

```powershell
.\gradlew.bat test
.\gradlew.bat :sample-jvm-winui3:run
```

The sample prints guidance when the process is not running on Windows.
