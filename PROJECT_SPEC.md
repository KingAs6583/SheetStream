# SheetStream — Project Requirements & Architecture Specification

> **Project Name:** `SheetStream`  
> **Target Ecosystem:** Kotlin (JVM & Android API 21+)  
> **Package Name:** `com.fidluc.sheetstream`  
> **Maven Coordinates:** `com.fidluc:sheetstream:1.0.0`  
> **Repository Type:** Standalone Pure Kotlin Library (Zero-Dependency)


---

## 1. Problem Statement & Motivation

Mobile developers (especially on Android using Room DB and Jetpack Compose) need to export structured data (such as financial transactions, reports, invoices) to Excel (`.xlsx`) spreadsheets with:
- **Multiple tabs / sheets** (e.g. "Transactions", "Summary", "Category Breakdown")
- **Cell formatting** (headers, bold text, background colors, currency, dates)
- **Formulas with cross-sheet references** (e.g., `'Transactions'!C2` or `=SUM('Transactions'!D2:D50)`)

### Why Existing Solutions Fail on Mobile:
1. **Apache POI:** Bloats APK size by **+35 MB to 45 MB**, risks DEX method count limits, and builds an in-memory DOM tree that causes `OutOfMemoryError` (OOM) on devices when exporting large datasets.
2. **SpreadsheetML (Single-file XML):** Deprecated, triggers security warnings in modern Excel/Google Sheets, and doesn't handle modern multi-sheet workflows cleanly.
3. **CSV:** Lacks formatting, cannot support multiple sheets/tabs, and cannot store formulas.

---

## 2. Core Architectural Principles

1. **Zero Third-Party Dependencies:**
   - Pure Kotlin utilizing standard Java runtime I/O:
     - `java.util.zip.ZipOutputStream`
     - `java.util.zip.ZipEntry`
     - `java.io.OutputStreamWriter` (UTF-8)
   - Zero Android SDK imports inside the library core (takes raw `java.io.OutputStream`). Shields the library from Android Scoped Storage changes.

2. **$O(1)$ Constant Memory Streaming:**
   - Rows must be serialized and written **directly to the `ZipOutputStream`** as they are produced by Room cursors / paging sources.
   - **Never buffer full sheet DOMs in memory.**
   - Uses `t="inlineStr"`:
     ```xml
     <c r="A1" t="inlineStr"><is><t>Sample Text</t></is></c>
     ```
     This bypasses the need for a global `xl/sharedStrings.xml` table, which would otherwise force caching all strings in RAM.

3. **Strict OOXML Conformance:**
   Produces valid ECMA-376 `.xlsx` archives opening cleanly without recovery prompts in Microsoft Excel, Google Sheets, LibreOffice, and mobile viewers.
   - `[Content_Types].xml`
   - `_rels/.rels`
   - `xl/workbook.xml`
   - `xl/_rels/workbook.xml.rels`
   - `xl/styles.xml`
   - `xl/worksheets/sheet{1..N}.xml`

4. **Backward Compatibility:**
   - Compatible with Android API 21+ (Android 5.0 Lollipop through modern Android 15+).
   - Java 8 bytecode target (`sourceCompatibility = JavaVersion.VERSION_1_8`).

---

## 3. Implemented Capabilities

### A. Workbook & Sheet Lifecycle
- Upfront and dynamic sheet creation:
  ```kotlin
  SheetStream.build(outputStream) {
      sheet("Transactions") { ... }
      sheet("Summary") { ... }
  }
  ```
- Custom column widths per sheet (in character units):
  ```kotlin
  setColumnWidths(15.0, 25.0, 15.0)
  ```

### B. Cell Types & Formatting
- **Text:** Inline strings with XML special character escaping (`&`, `<`, `>`, `"`, `'`).
- **Numbers:** Int, Long, Double, Float, BigDecimal.
- **Booleans:** `1` / `0` with `t="b"`.
- **Formulas:** Cross-sheet reference syntax `=SUM('SheetName'!A1:A50)`.
- **Pre-baked Cell Styles (`CellStyle` enum):**
  - `DEFAULT`: Standard cell font and border.
  - `HEADER`: Dark Green background (`#1E4620`), bold white text, centered, thin border.
  - `SUBHEADER`: Subtle gray background (`#EEEEEE`), bold text.
  - `CURRENCY`: Number format `#,##0.00`.
  - `INTEGER`: Number format `#,##0`.
  - `DATE`: ISO Date `yyyy-mm-dd`.
  - `PERCENTAGE`: Number format `0.00%`.
  - `INCOME`: Bold green text (`#1B5E20`) + currency format.
  - `EXPENSE`: Bold red text (`#B71C1C`) + currency format.
  - `BOLD`: Generic bold font.

---

## 4. Current File Tree

```text
SheetStream/
├── build.gradle.kts                        # Pure Kotlin JVM + maven-publish
├── settings.gradle.kts                     # Project name: SheetStream
├── README.md                               # Documentation & quick start
├── PROJECT_SPEC.md                         # This architecture specification
├── gradle/wrapper/
│   └── gradle-wrapper.properties           # Gradle 8.7
└── src/
    ├── main/kotlin/com/fidluc/sheetstream/
    │   ├── SheetStream.kt                  # Workbook orchestrator & Zip stream manager
    │   ├── Worksheet.kt                    # Worksheet, RowScope streaming builders, streamPaged
    │   ├── CellStyle.kt                    # Pre-compiled style definitions
    │   ├── XmlTemplates.kt                 # OOXML XML static templates & escaping logic
    │   ├── AndroidIntegration.kt           # Room DB, SAF, and FileProvider export guides
    │   └── Main.kt                         # Runnable local test (generates sample file)
    └── test/kotlin/com/fidluc/sheetstream/
        └── SheetStreamTest.kt              # Automated tests & validation suite
```


---

## 5. Roadmap & Completed Milestones

### Completed Milestones:
- [x] **Android Compatibility (API 21+ through Android 15+):**
  - Java 8 bytecode target (`jvmTarget = JVM_1_8`) configured so any Android D8/R8 toolchain consumes it natively without dexing issues.
  - Zero Android SDK dependencies in core engine; operates purely on standard I/O streams.
- [x] **Android ContentResolver & Room Integration Helpers:**
  - Added `streamPaged(chunkSize, fetchChunk, rowBuilder)` to stream paginated Room DB queries with constant $O(1)$ memory.
  - Added `headerRow(...)`, `subheaderRow(...)`, and `rows(Iterable/Sequence)`.
  - Added `outputStream.sheetStream { ... }` and `file.sheetStream { ... }` extension functions.
  - Created [AndroidIntegration.kt](file:///d:/Projects/SheetStream/src/main/kotlin/com/fidluc/sheetstream/AndroidIntegration.kt) with complete integration guides for Room DB, SAF, and FileProvider.
- [x] **Custom Style Builder & Freeze Panes:**
  - Dynamic `customStyle { ... }` supporting custom RGB background fills, font colors, bold/italic, and number format strings.
  - `freezeHeaderRow()` and `freezePanes(rows, cols)` for mobile scrolling stickiness.
- [x] **Streaming SAX Reader Core:**
  - `SheetStream.read(...)`, `readSpreadsheet { ... }` supporting both `inlineStr` and Excel `sharedStrings.xml`.
  - Flyweight `RowRecord` with high-performance on-demand getters (`getString`, `getDouble`, `getInt`, `getBoolean`, `getFormula`).
- [x] **Cross-Validation Test Suite:**
  - Added `fastexcel-reader` to `testImplementation`.
  - Round-trip tests and third-party reader validation.
- [x] **Publishing Configuration:**
  - Added Maven publish POM metadata with Apache 2.0 license, developer info, and sourcesJar for JitPack and Maven Central.


