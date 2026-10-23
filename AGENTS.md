# Project Rules for SheetStream

## 1. Strict No-Emoji Policy
- Under no circumstances should emojis be added to this codebase, comments, docstrings, commit messages, pull requests, or markdown documentation.
- Maintain a strictly professional, clean plain-text standard.
- Use standard Markdown elements (such as `[x]`, `-`, `*`, or standard typography) instead of emoji symbols.

## 2. Core Architectural Invariants
- Zero third-party runtime dependencies in `build.gradle.kts`.
- Standard Java I/O only: `java.util.zip.ZipOutputStream`, `java.io.OutputStreamWriter`.
- $O(1)$ constant memory streaming: Never buffer entire sheets or workbooks in memory.
- Java 8 bytecode target: Keep `jvmTarget = JVM_1_8` in Kotlin compilation for universal Android API 21+ and JVM compatibility.
- Zero `android.*` imports in the core engine: The engine takes standard `java.io.OutputStream` and `java.io.File`.
- Strict OOXML conformance: Generated `.xlsx` files must open without warnings in Excel, LibreOffice, and Google Sheets.
