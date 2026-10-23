package com.fidluc.sheetstream

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * High-performance, streaming Excel (.xlsx) generator.
 *
 * Implements an O(1) memory footprint with zero 3rd-party dependencies.
 * Safe for mobile (Android API 21+) and high-volume background exports.
 */
class SheetStream(
    private val outputStream: OutputStream,
    private val preDeclaredSheetNames: List<String>? = null
) : AutoCloseable {


    private val zip = ZipOutputStream(outputStream)
    private val writer: Writer = OutputStreamWriter(zip, StandardCharsets.UTF_8)
    private val recordedSheetNames = mutableListOf<String>()
    private val customStyles = mutableListOf<CustomCellStyle>()
    private var isClosed = false
    private var currentWorksheet: Worksheet? = null


    init {
        // If sheet names are known upfront, write metadata at the beginning of the ZIP
        if (preDeclaredSheetNames != null && preDeclaredSheetNames.isNotEmpty()) {
            writeMetadataEntries(preDeclaredSheetNames)
        }
    }

    /**
     * Adds and streams a new worksheet into the Excel workbook.
     *
     * @param sheetName Display name for the tab in Excel.
     * @param block Lambda where you stream rows into the sheet.
     */
    fun sheet(sheetName: String, block: Worksheet.() -> Unit): SheetStream {
        check(!isClosed) { "SheetStream is already closed." }
        currentWorksheet?.finish()

        val nextIndex = recordedSheetNames.size + 1
        recordedSheetNames.add(sheetName)

        val entryName = "xl/worksheets/sheet$nextIndex.xml"
        zip.putNextEntry(ZipEntry(entryName))

        writer.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        writer.write("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        writer.flush()

        val worksheet = Worksheet(writer, sheetName, nextIndex)
        currentWorksheet = worksheet
        worksheet.block()
        worksheet.finish()

        zip.closeEntry()
        currentWorksheet = null
        return this
    }

    /**
     * Registers a dynamic custom style (custom RGB background, text color, bold, format).
     * Can be passed directly into any cell() call.
     */
    fun customStyle(block: CellStyleBuilder.() -> Unit): CellStyle {
        check(!isClosed) { "SheetStream is already closed." }
        val builder = CellStyleBuilder().apply(block)
        val nextIndex = 10 + customStyles.size
        val custom = builder.build(nextIndex)
        customStyles.add(custom)
        return custom
    }


    /**
     * Finishes writing all sheets and metadata, flushing the ZIP file to the output stream.
     */
    fun finish() {
        if (isClosed) return

        currentWorksheet?.finish()
        currentWorksheet = null

        // If sheet names were not pre-declared, write metadata now that we know all sheet names
        if (preDeclaredSheetNames == null) {
            writeMetadataEntries(recordedSheetNames)
        }

        writer.flush()
        zip.finish()
        isClosed = true
    }

    override fun close() {
        if (!isClosed) {
            finish()
        }
        zip.close()
    }

    private fun writeMetadataEntries(sheetNames: List<String>) {
        val count = sheetNames.size.coerceAtLeast(1)

        // 1. [Content_Types].xml
        writeZipEntry("[Content_Types].xml", XmlTemplates.buildContentTypes(count))

        // 2. _rels/.rels
        writeZipEntry("_rels/.rels", XmlTemplates.ROOT_RELS)

        // 3. xl/workbook.xml
        writeZipEntry("xl/workbook.xml", XmlTemplates.buildWorkbook(sheetNames))

        // 4. xl/_rels/workbook.xml.rels
        writeZipEntry("xl/_rels/workbook.xml.rels", XmlTemplates.buildWorkbookRels(count))

        // 5. xl/styles.xml
        writeZipEntry("xl/styles.xml", XmlTemplates.buildStylesXml(customStyles))
    }


    private fun writeZipEntry(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        writer.write(content)
        writer.flush()
        zip.closeEntry()
    }

    companion object {
        /**
         * Recommended entry point: Streams sheets into the given OutputStream.
         * Automatically finishes and flushes the archive.
         *
         * Usage:
         * ```kotlin
         * SheetStream.build(outputStream) {
         *     sheet("Transactions") { ... }
         *     sheet("Summary") { ... }
         * }
         * ```
         */
        inline fun build(outputStream: OutputStream, block: SheetStream.() -> Unit) {
            SheetStream(outputStream).use { it.block() }
        }

        /**
         * Creates a SheetStream with upfront sheet names (guarantees [Content_Types].xml is the first entry).
         */
        inline fun build(
            outputStream: OutputStream,
            sheetNames: List<String>,
            block: SheetStream.() -> Unit
        ) {
            SheetStream(outputStream, sheetNames).use { it.block() }
        }

        /**
         * Streams sheets directly into the given target File.
         */
        inline fun build(file: File, crossinline block: SheetStream.() -> Unit) {
            FileOutputStream(file).use { build(it, block) }
        }

        /**
         * Streams sheets directly into the given target File with upfront sheet names.
         */
        inline fun build(
            file: File,
            sheetNames: List<String>,
            crossinline block: SheetStream.() -> Unit
        ) {
            FileOutputStream(file).use { build(it, sheetNames, block) }
        }
    }
}


/**
 * Extension on OutputStream to fluently build an Excel spreadsheet.
 */
inline fun OutputStream.sheetStream(block: SheetStream.() -> Unit) {
    SheetStream.build(this, block)
}

/**
 * Extension on OutputStream to fluently build an Excel spreadsheet with upfront sheet names.
 */
inline fun OutputStream.sheetStream(sheetNames: List<String>, block: SheetStream.() -> Unit) {
    SheetStream.build(this, sheetNames, block)
}

/**
 * Extension on File to fluently build an Excel spreadsheet.
 */
inline fun File.sheetStream(crossinline block: SheetStream.() -> Unit) {
    SheetStream.build(this, block)
}

/**
 * Extension on File to fluently build an Excel spreadsheet with upfront sheet names.
 */
inline fun File.sheetStream(sheetNames: List<String>, crossinline block: SheetStream.() -> Unit) {
    SheetStream.build(this, sheetNames, block)
}

