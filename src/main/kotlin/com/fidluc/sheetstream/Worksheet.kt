package com.fidluc.sheetstream

import java.io.Writer

/**
 * Manages row-by-row streaming of a single Excel worksheet.
 * Rows are written directly to the underlying stream to maintain O(1) memory footprint.
 */
class Worksheet internal constructor(
    private val writer: Writer,
    val name: String,
    val sheetIndex: Int
) {
    private var currentRowIndex = 0
    private var hasStartedSheetData = false
    private val columnWidths = mutableMapOf<Int, Double>()

    /**
     * Sets explicit column widths (character units, standard Excel metric).
     * Must be called BEFORE writing the first row.
     *
     * Example: `setColumnWidths(1 to 15.0, 2 to 30.0, 3 to 18.0)`
     */
    fun setColumnWidths(vararg widths: Pair<Int, Double>) {
        check(!hasStartedSheetData) { "Column widths must be set before writing rows." }
        columnWidths.putAll(widths)
    }

    /**
     * Sets column widths using a simple list where list index 0 corresponds to Column A (1).
     */
    fun setColumnWidths(widths: List<Double>) {
        check(!hasStartedSheetData) { "Column widths must be set before writing rows." }
        widths.forEachIndexed { index, width ->
            columnWidths[index + 1] = width
        }
    }

    /**
     * Sets column widths using a vararg of doubles where index 0 is Column A.
     *
     * Example: `setColumnWidths(15.0, 25.0, 15.0)`
     */
    fun setColumnWidths(vararg widths: Double) {
        setColumnWidths(widths.toList())
    }

    private var freezeRows = 0
    private var freezeCols = 0

    /**
     * Freezes the first row (header row) so it remains visible when scrolling down.
     * Must be called before writing rows.
     */
    fun freezeHeaderRow() {
        freezePanes(rows = 1, cols = 0)
    }

    /**
     * Freezes the specified number of rows and/or columns.
     * Must be called before writing rows.
     */
    fun freezePanes(rows: Int = 0, cols: Int = 0) {
        check(!hasStartedSheetData) { "Freeze panes must be set before writing rows." }
        freezeRows = rows
        freezeCols = cols
    }

    private fun ensureSheetDataStarted() {
        if (!hasStartedSheetData) {
            if (freezeRows > 0 || freezeCols > 0) {
                val splitColLetter = XmlTemplates.columnLetter(freezeCols + 1)
                val splitRowNum = freezeRows + 1
                val topLeftCell = "$splitColLetter$splitRowNum"
                val activePane = when {
                    freezeRows > 0 && freezeCols > 0 -> "bottomRight"
                    freezeRows > 0 -> "bottomLeft"
                    else -> "topRight"
                }
                writer.write("""<sheetViews><sheetView workbookViewId="0"><pane""")
                if (freezeCols > 0) writer.write(""" xSplit="$freezeCols"""")
                if (freezeRows > 0) writer.write(""" ySplit="$freezeRows"""")
                writer.write(""" topLeftCell="$topLeftCell" activePane="$activePane" state="frozen"/>""")
                writer.write("""</sheetView></sheetViews>""")
            }
            if (columnWidths.isNotEmpty()) {
                writer.write("<cols>")
                columnWidths.toSortedMap().forEach { (col, width) ->
                    writer.write("""<col min="$col" max="$col" width="$width" customWidth="1"/>""")
                }
                writer.write("</cols>")
            }
            writer.write("<sheetData>")
            hasStartedSheetData = true
        }
    }


    companion object {
        const val MAX_ROWS_PER_SHEET = 1_048_576
        const val MAX_COLS_PER_ROW = 16_384
    }

    /**
     * Appends a new row to the sheet.
     * Throws SheetStreamLimitExceededException if attempting to exceed Excel's maximum row limit (1,048,576).
     */
    fun row(block: RowScope.() -> Unit) {
        if (currentRowIndex >= MAX_ROWS_PER_SHEET) {
            throw SheetStreamLimitExceededException(
                limitType = LimitType.ROW_COUNT,
                actualValue = (currentRowIndex + 1).toLong(),
                maxValue = MAX_ROWS_PER_SHEET.toLong(),
                message = "Worksheet exceeds Excel maximum row limit of $MAX_ROWS_PER_SHEET rows."
            )
        }
        ensureSheetDataStarted()
        currentRowIndex++
        writer.write("""<row r="$currentRowIndex">""")
        val scope = RowScope(writer, currentRowIndex)
        scope.block()
        writer.write("</row>")
    }


    /**
     * Appends a table header row with CellStyle.HEADER applied to all cells.
     */
    fun headerRow(vararg headers: String) {
        row {
            for (header in headers) {
                cell(header, CellStyle.HEADER)
            }
        }
    }

    /**
     * Appends a table header row with CellStyle.HEADER from a collection of headers.
     */
    fun headerRow(headers: Iterable<String>) {
        row {
            for (header in headers) {
                cell(header, CellStyle.HEADER)
            }
        }
    }

    /**
     * Appends a table subheader row with CellStyle.SUBHEADER applied to all cells.
     */
    fun subheaderRow(vararg subheaders: String) {
        row {
            for (sub in subheaders) {
                cell(sub, CellStyle.SUBHEADER)
            }
        }
    }

    /**
     * Appends a table subheader row with CellStyle.SUBHEADER from a collection.
     */
    fun subheaderRow(subheaders: Iterable<String>) {
        row {
            for (sub in subheaders) {
                cell(sub, CellStyle.SUBHEADER)
            }
        }
    }

    /**
     * Appends a simple row of values directly from vararg values.
     */
    fun row(vararg values: Any?, style: CellStyle = CellStyle.DEFAULT) {
        row {
            cells(*values, style = style)
        }
    }

    /**
     * Appends a simple row of values directly from an Iterable.
     */
    fun row(values: Iterable<Any?>, style: CellStyle = CellStyle.DEFAULT) {
        row {
            cells(values, style = style)
        }
    }

    /**
     * Streams an iterable of items row-by-row into the worksheet.
     */
    inline fun <T> rows(items: Iterable<T>, crossinline block: RowScope.(T) -> Unit) {
        for (item in items) {
            row { block(item) }
        }
    }

    /**
     * Streams a sequence of items row-by-row into the worksheet with O(1) memory.
     * Ideal for lazy sequences and database cursors.
     */
    inline fun <T> rows(items: Sequence<T>, crossinline block: RowScope.(T) -> Unit) {
        for (item in items) {
            row { block(item) }
        }
    }

    /**
     * Streams paginated data into the worksheet in chunks.
     * Specially designed for Android Room DB / SQLite queries (e.g. `LIMIT :limit OFFSET :offset`).
     * Keeps memory consumption constant O(1) regardless of total record count.
     *
     * @param chunkSize Number of records per chunk (default 500).
     * @param fetchChunk Function fetching records for (offset, limit). Return empty list when done.
     * @param rowBuilder Builder for mapping each record into cells.
     */
    inline fun <T> streamPaged(
        chunkSize: Int = 500,
        crossinline fetchChunk: (offset: Int, limit: Int) -> List<T>,
        crossinline rowBuilder: RowScope.(T) -> Unit
    ) {
        var offset = 0
        while (true) {
            val chunk = fetchChunk(offset, chunkSize)
            if (chunk.isEmpty()) break
            for (item in chunk) {
                row { rowBuilder(item) }
            }
            if (chunk.size < chunkSize) break
            offset += chunkSize
        }
    }

    /**
     * Streams an entire table with a header row followed by items.
     */
    inline fun <T> streamTable(
        headers: List<String>,
        items: Iterable<T>,
        crossinline rowBuilder: RowScope.(T) -> Unit
    ) {
        headerRow(headers)
        rows(items, rowBuilder)
    }

    internal fun finish() {
        ensureSheetDataStarted()
        writer.write("</sheetData></worksheet>")
        writer.flush()
    }
}

/**
 * Builder scope for adding cells within a row.
 */
class RowScope internal constructor(
    private val writer: Writer,
    private val rowNum: Int
) {
    private var currentColIndex = 0

    private fun nextColIndex(): Int {
        currentColIndex++
        if (currentColIndex > Worksheet.MAX_COLS_PER_ROW) {
            throw SheetStreamLimitExceededException(
                limitType = LimitType.COLUMN_COUNT,
                actualValue = currentColIndex.toLong(),
                maxValue = Worksheet.MAX_COLS_PER_ROW.toLong(),
                message = "Row exceeds Excel maximum column limit of ${Worksheet.MAX_COLS_PER_ROW} columns (Column XFD)."
            )
        }
        return currentColIndex
    }

    /**
     * Appends a text / string cell using inline strings (zero in-memory table required).
     */
    fun cell(value: String?, style: CellStyle = CellStyle.DEFAULT) {
        val col = nextColIndex()
        if (value == null) {
            writeEmpty(col, style)
            return
        }
        val ref = "${XmlTemplates.columnLetter(col)}$rowNum"

        val escaped = XmlTemplates.escapeXml(value)
        val styleAttr = if (style != CellStyle.DEFAULT) """ s="${style.index}"""" else ""
        writer.write("""<c r="$ref"$styleAttr t="inlineStr"><is><t>$escaped</t></is></c>""")
    }

    /**
     * Appends a numeric cell (Double, Long, Int, Float, BigDecimal).
     */
    fun cell(value: Number?, style: CellStyle = CellStyle.DEFAULT) {
        val col = nextColIndex()
        if (value == null) {
            writeEmpty(col, style)
            return
        }
        val ref = "${XmlTemplates.columnLetter(col)}$rowNum"

        val styleAttr = if (style != CellStyle.DEFAULT) """ s="${style.index}"""" else ""
        writer.write("""<c r="$ref"$styleAttr><v>$value</v></c>""")
    }

    /**
     * Appends a boolean cell (TRUE / FALSE).
     */
    fun cell(value: Boolean, style: CellStyle = CellStyle.DEFAULT) {
        val col = nextColIndex()
        val ref = "${XmlTemplates.columnLetter(col)}$rowNum"

        val styleAttr = if (style != CellStyle.DEFAULT) """ s="${style.index}"""" else ""
        val intVal = if (value) 1 else 0
        writer.write("""<c r="$ref"$styleAttr t="b"><v>$intVal</v></c>""")
    }

    /**
     * Appends an Excel formula cell.
     *
     * Example:
     * - `formula("SUM(C2:C50)", style = CellStyle.CURRENCY)`
     * - `formula("'Transactions'!C2", style = CellStyle.CURRENCY)`
     */
    fun formula(
        formulaText: String,
        style: CellStyle = CellStyle.DEFAULT,
        cachedValue: Number? = null
    ) {
        val col = nextColIndex()
        val ref = "${XmlTemplates.columnLetter(col)}$rowNum"

        val styleAttr = if (style != CellStyle.DEFAULT) """ s="${style.index}"""" else ""
        val cleanFormula = XmlTemplates.escapeXml(formulaText.trim().removePrefix("="))

        if (cachedValue != null) {
            writer.write("""<c r="$ref"$styleAttr><f>$cleanFormula</f><v>$cachedValue</v></c>""")
        } else {
            writer.write("""<c r="$ref"$styleAttr><f>$cleanFormula</f></c>""")
        }
    }

    /**
     * Skips one or more columns with empty cells.
     */
    fun skip(count: Int = 1) {
        currentColIndex += count
        if (currentColIndex > Worksheet.MAX_COLS_PER_ROW) {
            throw SheetStreamLimitExceededException(
                limitType = LimitType.COLUMN_COUNT,
                actualValue = currentColIndex.toLong(),
                maxValue = Worksheet.MAX_COLS_PER_ROW.toLong(),
                message = "Row exceeds Excel maximum column limit of ${Worksheet.MAX_COLS_PER_ROW} columns (Column XFD)."
            )
        }
    }

    /**
     * Appends an empty cell, optionally with borders or background style.
     */
    fun emptyCell(style: CellStyle = CellStyle.DEFAULT) {
        val col = nextColIndex()
        writeEmpty(col, style)
    }


    /**
     * Universal cell builder accepting any supported type: String, Number, Boolean, or null.
     */
    fun cellAny(value: Any?, style: CellStyle = CellStyle.DEFAULT) {
        when (value) {
            null -> emptyCell(style)
            is String -> cell(value, style)
            is Number -> cell(value, style)
            is Boolean -> cell(value, style)
            else -> cell(value.toString(), style)
        }
    }

    /**
     * Appends multiple cells in a row from vararg values.
     */
    fun cells(vararg values: Any?, style: CellStyle = CellStyle.DEFAULT) {
        for (v in values) {
            cellAny(v, style)
        }
    }

    /**
     * Appends multiple cells in a row from an Iterable.
     */
    fun cells(values: Iterable<Any?>, style: CellStyle = CellStyle.DEFAULT) {
        for (v in values) {
            cellAny(v, style)
        }
    }

    private fun writeEmpty(col: Int, style: CellStyle) {
        if (style == CellStyle.DEFAULT) return
        val ref = "${XmlTemplates.columnLetter(col)}$rowNum"
        writer.write("""<c r="$ref" s="${style.index}"/>""")
    }
}
