# SheetStream

> **Zero-dependency, O(1) memory, streaming Excel (.xlsx) generator for Kotlin & Android.**

SheetStream is an ultra-lightweight library designed specifically for mobile apps (and backend services) that need to export multi-tab spreadsheets with styling, numbers, and cross-sheet formulas without bloating the APK or crashing the phone with OutOfMemoryError.

---

## Why SheetStream?

| Feature | Apache POI | FastExcel | SheetStream |
| :--- | :--- | :--- | :--- |
| **APK Size Impact** | +35 MB to 45 MB (High) | ~120 KB | **~25 KB (0 KB dependencies)** |
| **Android Compatibility** | Risky (OOM & dex limits) | Good | **100% Native (API 21+)** |
| **Memory Consumption** | High (in-memory DOM tree) | Low | **O(1) Constant (Direct Zip stream)** |
| **Dependencies** | 10+ (XmlBeans, Commons) | 0 | **0 (Only standard Java/Kotlin I/O)** |
| **Multi-tab / Sheets** | Yes | Yes | **Yes** |
| **Cell Formatting & Colors** | Complex | Basic | **Pre-baked & Instant** |

---

## Installation

### Option 1: JitPack (Recommended for Gradle projects)

Add JitPack repository to your root `settings.gradle.kts` (or root `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = java.net.URI("https://jitpack.io") }
    }
}
```

Add SheetStream dependency to your app module `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.KingAs6583:SheetStream:1.0.0")
}
```

### Option 2: Direct GitHub Releases (JAR)

1. Download `sheetstream-1.0.0.jar` from the GitHub Releases page.
2. Place the JAR into your Android project's `app/libs/` directory.
3. In `app/build.gradle.kts`, add:

```kotlin
dependencies {
    implementation(files("libs/sheetstream-1.0.0.jar"))
}
```

---

## Quick Usage

```kotlin
import com.fidluc.sheetstream.SheetStream
import com.fidluc.sheetstream.CellStyle
import com.fidluc.sheetstream.sheetStream
import java.io.FileOutputStream

val fileOutputStream = FileOutputStream("Export.xlsx")

SheetStream.build(fileOutputStream) {

    // Tab 1: Transactions
    sheet("Transactions") {
        setColumnWidths(15.0, 25.0, 15.0)

        row {
            cell("Date", CellStyle.HEADER)
            cell("Category", CellStyle.HEADER)
            cell("Amount", CellStyle.HEADER)
        }

        // Stream rows directly from your Room DB or cursor
        transactions.forEach { tx ->
            row {
                cell(tx.date, CellStyle.DATE)
                cell(tx.category)
                val style = if (tx.amount >= 0) CellStyle.INCOME else CellStyle.EXPENSE
                cell(tx.amount, style)
            }
        }
    }

    // Tab 2: Financial Summary
    sheet("Summary") {
        row {
            cell("Total Income", CellStyle.BOLD)
            formula("SUMIF('Transactions'!C2:C100, \">0\")", CellStyle.CURRENCY)
        }
        row {
            cell("Net Balance", CellStyle.BOLD)
            formula("SUM('Transactions'!C2:C100)", CellStyle.CURRENCY)
        }
    }
}
```

---

## Built-in Cell Styles

| Style | Appearance / Formatting |
| :--- | :--- |
| `CellStyle.DEFAULT` | Standard text / numbers |
| `CellStyle.HEADER` | Dark Green fill (#1E4620), Bold white text, centered, thin border |
| `CellStyle.SUBHEADER` | Subtle gray fill (#EEEEEE), Bold text, thin border |
| `CellStyle.CURRENCY` | Currency formatted (`#,##0.00`) |
| `CellStyle.INTEGER` | Number formatted (`#,##0`) |
| `CellStyle.DATE` | ISO Date (`yyyy-mm-dd`) |
| `CellStyle.PERCENTAGE`| Percentage (`0.00%`) |
| `CellStyle.INCOME` | Bold green text (#1B5E20) + Currency |
| `CellStyle.EXPENSE` | Bold red text (#B71C1C) + Currency |
| `CellStyle.BOLD` | General bold text |

### Dynamic Custom Styles & Freeze Panes

Define custom corporate colors and freeze table headers with zero XML overhead:

```kotlin
outputFile.sheetStream {
    val brandHeader = customStyle {
        backgroundColor = "#0D47A1"  // Custom hex background
        textColor = "#FFFFFF"        // White bold text
        bold = true
        centered = true
    }

    sheet("Monthly Report") {
        freezeHeaderRow() // Keeps header visible when scrolling on mobile
        row {
            cell("Date", brandHeader)
            cell("Amount", brandHeader)
        }
    }
}
```

---

## Reading / Importing Spreadsheets

Read any `.xlsx` spreadsheet (created by Microsoft Excel, Google Sheets, or SheetStream) row-by-row with $O(1)$ constant memory and zero third-party dependencies:

```kotlin
// Read directly from a File or Android ContentResolver InputStream
inputFile.readSpreadsheet {
    sheet("Transactions") {
        forEachRow { row ->
            // High-performance typed accessors (by column letter or 1-based index)
            val date = row.getString("A")       // or row.getString(1)
            val category = row.getString("B")   // or row.getString(2)
            val amount = row.getDouble("C")     // or row.getDouble(3)
            val isVerified = row.getBoolean("D")

            // Insert directly into Room DB entity
            if (date != null && amount != null) {
                transactionDao.insert(Transaction(date, category ?: "", amount))
            }
        }
    }
}
```

---

## Integration into Android (e.g. Room DB & ContentResolver)

Works seamlessly on **any Android version (API 21+ through Android 15+)** with **Java 8 bytecode** compatibility and zero dependencies.

In your Android `ViewModel` or `Repository`:

```kotlin
fun exportReport(context: Context, destinationUri: Uri) {
    context.contentResolver.openOutputStream(destinationUri)?.sheetStream {
        sheet("Transactions") {
            setColumnWidths(15.0, 20.0, 30.0, 15.0)
            freezeHeaderRow()

            // Fast header row with automatic styling
            headerRow("Date", "Category", "Note", "Amount")

            // Stream paginated data in chunks of 500 directly from Room DB
            // Keeps memory O(1) constant even with 100,000+ records
            streamPaged(
                chunkSize = 500,
                fetchChunk = { offset, limit -> transactionDao.getChunk(limit, offset) }
            ) { tx ->
                cell(tx.formattedDate, CellStyle.DATE)
                cell(tx.categoryName)
                cell(tx.note)
                val style = if (tx.isIncome) CellStyle.INCOME else CellStyle.EXPENSE
                cell(tx.amount, style)
            }
        }
    }
}
```

---

## Testing Locally (Without Android Emulator)

You can run the demonstration directly from the command line or within your IDE:

### Command Line
```bash
./gradlew run
```

### Android Studio / IntelliJ IDEA
1. Open this project in **Android Studio** or **IntelliJ IDEA**.
2. Navigate to `src/main/kotlin/com/fidluc/sheetstream/Main.kt`.
3. Click the green **Play** icon next to `fun main()`.
4. It will immediately generate `sample_financial_report.xlsx` and stream-read it back to the console.
5. Open the generated file in Microsoft Excel, LibreOffice, or Google Sheets to inspect the output.

---

## Package Structure & Architecture Rationale

SheetStream intentionally uses a flat package structure:

```text
com.fidluc.sheetstream
├── SheetStream.kt     (High-level builder & ZIP coordinator)
├── Worksheet.kt       (Row-by-row XML streaming)
├── CellStyle.kt       (Predefined & custom RGB styling)
├── SheetReader.kt     (SAX streaming parser)
├── RowRecord.kt       (Flyweight typed row accessors)
├── Exceptions.kt      (Safety limit exceptions & ReaderConfig)
└── XmlTemplates.kt    (Internal OOXML structure generators)
```

### Why a Flat Structure is Best for Developers:

1. **Single Import Ergonomics**: Consumers only need `import com.fidluc.sheetstream.*` to gain full
   access to writers, readers, builders, styles, and exceptions without fragmenting across
   subpackages.
2. **Clean Encapsulation**: Low-level implementation details (`XmlTemplates`, `SheetMetadata`) are
   marked Kotlin `internal`, preventing public API pollution.
3. **Android DEX & Inlining Optimization**: Avoids synthetic cross-package accessor methods in
   bytecode, keeping binary size under 30 KB.

---

## Documentation & Project Links

- [Changelog](CHANGELOG.md) - Complete version history and release notes.
- [Maintainer Release Guide](RELEASING.md) - Instructions for cutting tags, automated GitHub
  Releases, and Maven Central deployment.
- [Contributing Guidelines](CONTRIBUTING.md) - Code style, architectural invariants, and pull
  request checklist.
- [License](LICENSE) - Apache License 2.0.


