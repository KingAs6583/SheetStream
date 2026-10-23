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
5. Open the generated file in Microsoft Excel, LibreOffice, or Google Sheets to inspect the output!


