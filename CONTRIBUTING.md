# Contributing to SheetStream

Thank you for your interest in contributing to SheetStream.

## Design Principles and Invariants

All contributions must respect the following core technical principles:

1. **Zero External Dependencies**: The core engine must never introduce third-party libraries. Only standard Java/Kotlin runtime I/O is allowed (`java.util.zip.ZipOutputStream`, `java.io.OutputStreamWriter`).
2. **O(1) Constant Memory**: Data must be written directly to the zip output stream as rows are produced. Never buffer DOM representations of entire sheets in memory.
3. **Java 8 Bytecode Target**: The library targets Java 8 (`JVM_1_8`) for universal Android (API 21+) and JVM compatibility. Do not use language features or APIs that require Java 9+ bytecode without desugaring.
4. **No Emojis**: Do not use emojis anywhere in the codebase, comments, commit messages, PR titles, or documentation.
5. **No Android SDK in Core**: Keep core engine files free of `android.*` imports so SheetStream remains fully platform-agnostic.

## Development Workflow

1. Fork the repository and create a feature branch:
   ```bash
   git checkout -b feature/my-feature
   ```
2. Make your changes adhering to existing Kotlin code conventions.
3. Run the automated test suite:
   ```bash
   ./gradlew test
   ```
4. Ensure all tests pass and no unnecessary files are staged.
5. Submit a Pull Request with a clear description of the problem solved.
