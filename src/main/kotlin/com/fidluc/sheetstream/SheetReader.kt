package com.fidluc.sheetstream

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

/**
 * Streaming Excel (.xlsx) reader using standard Java/Android SAX parser.
 * Maintains an O(1) constant memory profile with zero third-party dependencies.
 */
class SheetReader internal constructor(
    private val zipFile: ZipFile,
    private val config: ReaderConfig = ReaderConfig()
) : AutoCloseable {

    private val sheetMap = mutableListOf<SheetMetadata>()
    private val sharedStrings = mutableListOf<String>()

    init {
        loadWorkbookMetadata()
        loadSharedStrings()
    }


    private fun loadWorkbookMetadata() {
        val entry = zipFile.getEntry("xl/workbook.xml") ?: return
        zipFile.getInputStream(entry).use { stream ->
            val parser = createSecureSaxParser()
            parser.parse(InputSource(stream), object : DefaultHandler() {
                var index = 1
                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    val tag = localName ?: qName ?: ""
                    if (tag.equals("sheet", ignoreCase = true)) {
                        val name = attributes?.getValue("name") ?: "Sheet$index"
                        val sheetId = attributes?.getValue("sheetId") ?: "$index"
                        sheetMap.add(SheetMetadata(name = name, sheetIndex = index, entryPath = "xl/worksheets/sheet$index.xml"))
                        index++
                    }
                }
            })
        }
    }

    private fun loadSharedStrings() {
        val entry = zipFile.getEntry("xl/sharedStrings.xml") ?: return
        zipFile.getInputStream(entry).use { stream ->
            val parser = createSecureSaxParser()
            parser.parse(InputSource(stream), object : DefaultHandler() {
                private var inSi = false
                private var inT = false
                private val currentText = StringBuilder()

                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    val tag = localName ?: qName ?: ""
                    if (tag.equals("si", ignoreCase = true)) {
                        inSi = true
                        currentText.setLength(0)
                    } else if (inSi && tag.equals("t", ignoreCase = true)) {
                        inT = true
                    }
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    if (inT) {
                        currentText.append(ch, start, length)
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    val tag = localName ?: qName ?: ""
                    if (tag.equals("t", ignoreCase = true)) {
                        inT = false
                    } else if (tag.equals("si", ignoreCase = true)) {
                        inSi = false
                        if (sharedStrings.size >= config.maxSharedStrings) {
                            throw SheetStreamLimitExceededException(
                                limitType = LimitType.SHARED_STRINGS,
                                actualValue = (sharedStrings.size + 1).toLong(),
                                maxValue = config.maxSharedStrings.toLong(),
                                message = "Reading aborted: sharedStrings.xml contains more than ${config.maxSharedStrings} unique strings. Configure ReaderConfig.maxSharedStrings to allow larger string tables."
                            )
                        }
                        sharedStrings.add(currentText.toString())
                    }
                }

            })
        }
    }

    internal fun readSheet(metadata: SheetMetadata, rowCallback: (RowRecord) -> Unit) {
        val entry = zipFile.getEntry(metadata.entryPath)
            ?: zipFile.getEntry("xl/worksheets/sheet${metadata.sheetIndex}.xml")
            ?: return

        zipFile.getInputStream(entry).use { stream ->
            val parser = createSecureSaxParser()
            parser.parse(InputSource(stream), object : DefaultHandler() {
                private var currentRowNum = 0
                private var currentColIndex = 0
                private var cellType: String? = null
                private var inFormula = false
                private var inValue = false
                private var inInlineText = false
                private val currentFormula = StringBuilder()
                private val currentValue = StringBuilder()
                private val rowValues = mutableMapOf<Int, String?>()
                private val rowFormulas = mutableMapOf<Int, String>()

                override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                    val tag = localName ?: qName ?: ""
                    when {
                        tag.equals("row", ignoreCase = true) -> {
                            currentRowNum = attributes?.getValue("r")?.toIntOrNull() ?: (currentRowNum + 1)
                            if (currentRowNum > config.maxRowsPerSheet) {
                                throw SheetStreamLimitExceededException(
                                    limitType = LimitType.ROW_COUNT,
                                    actualValue = currentRowNum.toLong(),
                                    maxValue = config.maxRowsPerSheet.toLong(),
                                    message = "Reading aborted: worksheet exceeds maximum row limit of ${config.maxRowsPerSheet} rows. Configure ReaderConfig.maxRowsPerSheet to allow larger sheets."
                                )
                            }
                            rowValues.clear()
                            rowFormulas.clear()
                        }

                        tag.equals("c", ignoreCase = true) -> {
                            val r = attributes?.getValue("r")
                            currentColIndex = if (r != null) parseColumnIndex(r) else (currentColIndex + 1)
                            cellType = attributes?.getValue("t")
                            currentFormula.setLength(0)
                            currentValue.setLength(0)
                        }
                        tag.equals("f", ignoreCase = true) -> {
                            inFormula = true
                            currentFormula.setLength(0)
                        }
                        tag.equals("v", ignoreCase = true) -> {
                            inValue = true
                            currentValue.setLength(0)
                        }
                        tag.equals("t", ignoreCase = true) -> {
                            inInlineText = true
                        }
                    }
                }

                override fun characters(ch: CharArray, start: Int, length: Int) {
                    when {
                        inFormula -> currentFormula.append(ch, start, length)
                        inValue -> currentValue.append(ch, start, length)
                        inInlineText -> currentValue.append(ch, start, length)
                    }
                }

                override fun endElement(uri: String?, localName: String?, qName: String?) {
                    val tag = localName ?: qName ?: ""
                    when {
                        tag.equals("f", ignoreCase = true) -> inFormula = false
                        tag.equals("v", ignoreCase = true) -> inValue = false
                        tag.equals("t", ignoreCase = true) -> inInlineText = false
                        tag.equals("c", ignoreCase = true) -> {
                            val rawVal = currentValue.toString()
                            val resolvedVal = when (cellType) {
                                "s" -> {
                                    val idx = rawVal.toIntOrNull()
                                    if (idx != null && idx in 0 until sharedStrings.size) sharedStrings[idx] else rawVal
                                }
                                "b" -> if (rawVal == "1") "true" else "false"
                                else -> rawVal
                            }
                            rowValues[currentColIndex] = resolvedVal
                            if (currentFormula.isNotEmpty()) {
                                rowFormulas[currentColIndex] = currentFormula.toString()
                            }
                        }
                        tag.equals("row", ignoreCase = true) -> {
                            val record = RowRecord(currentRowNum, rowValues.toMap(), rowFormulas.toMap())
                            rowCallback(record)
                        }
                    }
                }

                private fun parseColumnIndex(cellRef: String): Int {
                    val letters = StringBuilder()
                    for (ch in cellRef) {
                        if (ch.isLetter()) letters.append(ch) else break
                    }
                    return RowRecord.columnNumber(letters.toString())
                }
            })
        }
    }

    internal val sheetNames: List<String> get() = sheetMap.map { it.name }

    internal fun findSheet(name: String): SheetMetadata? {
        return sheetMap.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    internal fun getSheetAt(index: Int): SheetMetadata? {
        return sheetMap.getOrNull(index)
    }

    override fun close() {
        zipFile.close()
    }

    companion object {
        private fun createSecureSaxParser(): javax.xml.parsers.SAXParser {
            val factory = SAXParserFactory.newInstance()
            factory.isNamespaceAware = true
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            } catch (_: Exception) {}
            return factory.newSAXParser()
        }
    }
}

internal data class SheetMetadata(
    val name: String,
    val sheetIndex: Int,
    val entryPath: String
)

/**
 * DSL scope for reading sheets and rows from an Excel spreadsheet.
 */
class SheetReaderScope internal constructor(private val reader: SheetReader) {

    /**
     * List of all sheet tab names discovered in the spreadsheet.
     */
    val sheetNames: List<String> get() = reader.sheetNames

    /**
     * Reads a specific sheet by tab name.
     */
    fun sheet(name: String, block: ReadSheetScope.() -> Unit) {
        val meta = reader.findSheet(name) ?: return
        val scope = ReadSheetScope(reader, meta)
        scope.block()
    }

    /**
     * Reads a specific sheet by 0-based index.
     */
    fun sheet(index: Int, block: ReadSheetScope.() -> Unit) {
        val meta = reader.getSheetAt(index) ?: return
        val scope = ReadSheetScope(reader, meta)
        scope.block()
    }

    /**
     * Iterates over every sheet in the workbook in order.
     */
    fun forEachSheet(block: ReadSheetScope.(sheetName: String) -> Unit) {
        for (name in sheetNames) {
            val meta = reader.findSheet(name) ?: continue
            val scope = ReadSheetScope(reader, meta)
            scope.block(name)
        }
    }
}

/**
 * Scope for reading rows within a specific worksheet.
 */
class ReadSheetScope internal constructor(
    private val reader: SheetReader,
    private val metadata: SheetMetadata
) {
    val name: String get() = metadata.name

    /**
     * Streams each row in the sheet to the callback one by one with O(1) memory.
     */
    fun forEachRow(block: (row: RowRecord) -> Unit) {
        reader.readSheet(metadata, block)
    }

    /**
     * Reads all rows in this sheet into an in-memory list (convenience for small sheets).
     */
    fun toRowList(): List<RowRecord> {
        val list = mutableListOf<RowRecord>()
        forEachRow { list.add(it) }
        return list
    }
}

/**
 * Reads an Excel spreadsheet from the given File.
 */
fun SheetStream.Companion.read(
    file: File,
    config: ReaderConfig = ReaderConfig(),
    block: SheetReaderScope.() -> Unit
) {
    if (file.length() > config.maxFileSizeBytes) {
        throw SheetStreamLimitExceededException(
            limitType = LimitType.FILE_SIZE,
            actualValue = file.length(),
            maxValue = config.maxFileSizeBytes,
            message = "File size of ${file.length()} bytes exceeds configured limit of ${config.maxFileSizeBytes} bytes. Configure ReaderConfig.maxFileSizeBytes to allow larger files."
        )
    }

    try {
        ZipFile(file).use { zip ->
            val reader = SheetReader(zip, config)
            SheetReaderScope(reader).block()
        }
    } catch (e: SheetStreamException) {
        throw e
    } catch (e: Exception) {
        throw InvalidSpreadsheetException("Failed to read Excel file: ${e.message}", e)
    }
}

/**
 * Reads an Excel spreadsheet from the given InputStream.
 * Safely buffers to a temporary cache file to enable random entry access with O(1) memory.
 */
fun SheetStream.Companion.read(
    inputStream: InputStream,
    config: ReaderConfig = ReaderConfig(),
    block: SheetReaderScope.() -> Unit
) {
    val tempFile = File.createTempFile("sheetstream_read_", ".xlsx")
    try {
        var totalBytes = 0L
        tempFile.outputStream().use { out ->
            val buffer = ByteArray(8192)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                totalBytes += bytesRead
                if (totalBytes > config.maxFileSizeBytes) {
                    throw SheetStreamLimitExceededException(
                        limitType = LimitType.FILE_SIZE,
                        actualValue = totalBytes,
                        maxValue = config.maxFileSizeBytes,
                        message = "Input stream size exceeds configured limit of ${config.maxFileSizeBytes} bytes."
                    )
                }
                out.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
        }
        read(tempFile, config, block)
    } finally {
        tempFile.delete()
    }
}

/**
 * Extension on File to fluently read an Excel spreadsheet.
 */
fun File.readSpreadsheet(
    config: ReaderConfig = ReaderConfig(),
    block: SheetReaderScope.() -> Unit
) {
    SheetStream.read(this, config, block)
}

/**
 * Extension on InputStream to fluently read an Excel spreadsheet.
 */
fun InputStream.readSpreadsheet(
    config: ReaderConfig = ReaderConfig(),
    block: SheetReaderScope.() -> Unit
) {
    SheetStream.read(this, config, block)
}

