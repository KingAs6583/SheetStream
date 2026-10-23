package com.fidluc.sheetstream

/**
 * High-performance, lightweight representation of an Excel row.
 * Uses on-demand typed accessors to prevent memory allocation overhead.
 */
class RowRecord internal constructor(
    val rowNum: Int,
    private val cellValues: Map<Int, String?>,
    private val cellFormulas: Map<Int, String>
) {
    /**
     * 1-based row index in the worksheet.
     */
    val rowNumber: Int get() = rowNum

    /**
     * Total number of populated columns in this row.
     */
    val columnCount: Int get() = cellValues.keys.maxOrNull() ?: 0

    /**
     * Gets cell string value by 1-based column index (1 = Column A, 2 = Column B).
     */
    fun getString(colIndex: Int): String? = cellValues[colIndex]

    /**
     * Gets cell string value by column letter (e.g. "A", "B", "AA").
     */
    fun getString(colLetter: String): String? = getString(columnNumber(colLetter))

    /**
     * Gets cell value parsed as Double, or null if cell is empty or non-numeric.
     */
    fun getDouble(colIndex: Int): Double? = getString(colIndex)?.trim()?.toDoubleOrNull()

    /**
     * Gets cell value parsed as Double by column letter.
     */
    fun getDouble(colLetter: String): Double? = getDouble(columnNumber(colLetter))

    /**
     * Gets cell value parsed as Int, or null if empty or non-numeric.
     */
    fun getInt(colIndex: Int): Int? = getDouble(colIndex)?.toInt()

    /**
     * Gets cell value parsed as Int by column letter.
     */
    fun getInt(colLetter: String): Int? = getInt(columnNumber(colLetter))

    /**
     * Gets cell value parsed as Long, or null if empty or non-numeric.
     */
    fun getLong(colIndex: Int): Long? = getDouble(colIndex)?.toLong()

    /**
     * Gets cell value parsed as Long by column letter.
     */
    fun getLong(colLetter: String): Long? = getLong(columnNumber(colLetter))

    /**
     * Gets cell value parsed as Boolean, or null if empty.
     */
    fun getBoolean(colIndex: Int): Boolean? {
        val str = getString(colIndex)?.trim() ?: return null
        return when {
            str == "1" || str.equals("true", ignoreCase = true) -> true
            str == "0" || str.equals("false", ignoreCase = true) -> false
            else -> null
        }
    }

    /**
     * Gets cell value parsed as Boolean by column letter.
     */
    fun getBoolean(colLetter: String): Boolean? = getBoolean(columnNumber(colLetter))

    /**
     * Gets the raw formula string of the cell if one was defined (e.g. "SUM(A1:A10)"), without leading '='.
     */
    fun getFormula(colIndex: Int): String? = cellFormulas[colIndex]

    /**
     * Gets the raw formula string by column letter.
     */
    fun getFormula(colLetter: String): String? = getFormula(columnNumber(colLetter))

    /**
     * Converts this row into a List of String values from Column 1 to the highest populated column.
     * Empty cells in between are represented as null.
     */
    fun toList(): List<String?> {
        val maxCol = columnCount
        if (maxCol == 0) return emptyList()
        return (1..maxCol).map { col -> getString(col) }
    }

    companion object {
        /**
         * Converts standard Excel column letters (A, B, Z, AA) to 1-based index (1, 2, 26, 27).
         */
        fun columnNumber(letter: String): Int {
            var result = 0
            for (ch in letter.uppercase()) {
                if (ch in 'A'..'Z') {
                    result = result * 26 + (ch - 'A' + 1)
                }
            }
            return result
        }
    }
}
