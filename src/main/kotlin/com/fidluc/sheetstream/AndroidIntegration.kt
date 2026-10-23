package com.fidluc.sheetstream

import java.io.File
import java.io.OutputStream

/**
 * Android Integration Guide & Helper Utilities for SheetStream.
 *
 * SheetStream is designed to be 100% compatible with ANY Android version (API 21+ through Android 15+).
 * It uses Java 8 bytecode, requires 0 Android SDK imports in the core engine, and streams directly to
 * any [java.io.OutputStream], completely avoiding OutOfMemoryErrors (OOM).
 *
 * ---
 *
 * ### 1. Room Database Integration (Constant O(1) Memory)
 *
 * In your Room `@Dao`:
 * ```kotlin
 * @Dao
 * interface TransactionDao {
 *     @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit OFFSET :offset")
 *     fun getPaged(limit: Int, offset: Int): List<TransactionEntity>
 * }
 * ```
 *
 * In your `ViewModel` or `Repository`:
 * ```kotlin
 * fun exportFromRoom(dao: TransactionDao, outputStream: OutputStream) {
 *     outputStream.sheetStream {
 *         sheet("Transactions") {
 *             setColumnWidths(15.0, 20.0, 30.0, 15.0)
 *             headerRow("Date", "Category", "Note", "Amount")
 *
 *             // Streams data in chunks of 500 without buffering the whole table in RAM
 *             streamPaged(
 *                 chunkSize = 500,
 *                 fetchChunk = { offset, limit -> dao.getPaged(limit, offset) }
 *             ) { tx ->
 *                 cell(tx.date, CellStyle.DATE)
 *                 cell(tx.category)
 *                 cell(tx.note)
 *                 val style = if (tx.amount >= 0) CellStyle.INCOME else CellStyle.EXPENSE
 *                 cell(tx.amount, style)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ---
 *
 * ### 2. Storage Access Framework (SAF) / ContentResolver (User saves to Downloads / Drive)
 *
 * When the user selects a save location via `ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")`:
 * ```kotlin
 * fun exportToSafUri(context: Context, destinationUri: Uri) {
 *     context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
 *         outputStream.sheetStream {
 *             sheet("Report") {
 *                 headerRow("ID", "Name", "Total")
 *                 // Write rows...
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ---
 *
 * ### 3. Cache Directory & FileProvider (Instant Sharing via WhatsApp, Gmail, etc.)
 *
 * To export and share a spreadsheet file without asking the user for folder permissions:
 * ```kotlin
 * fun shareSpreadsheet(context: Context) {
 *     val exportFile = File(context.cacheDir, "Financial_Report_${System.currentTimeMillis()}.xlsx")
 *
 *     exportFile.sheetStream {
 *         sheet("Summary") {
 *             headerRow("Month", "Revenue", "Expenses")
 *             // Write rows...
 *         }
 *     }
 *
 *     val contentUri = FileProvider.getUriForFile(
 *         context,
 *         "${context.packageName}.fileprovider",
 *         exportFile
 *     )
 *
 *     val shareIntent = Intent(Intent.ACTION_SEND).apply {
 *         type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
 *         putExtra(Intent.EXTRA_STREAM, contentUri)
 *         addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
 *     }
 *     context.startActivity(Intent.createChooser(shareIntent, "Share Report via..."))
 * }
 * ```
 */
object AndroidIntegrationGuide {
    const val MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val DEFAULT_FILE_NAME = "Report.xlsx"
}
