package com.fidluc.sheetstream

/**
 * Cell style contract mapping to a style index in xl/styles.xml.
 */
interface CellStyle {
    val index: Int

    companion object {
        /** Plain text or number with default font and borders */
        val DEFAULT: CellStyle = PredefinedStyle.DEFAULT

        /** Table header: White bold text, Dark Green (#1E4620) background, centered */
        val HEADER: CellStyle = PredefinedStyle.HEADER

        /** Section subheader: Black bold text, subtle gray background (#EEEEEE) */
        val SUBHEADER: CellStyle = PredefinedStyle.SUBHEADER

        /** Currency formatted number: `#,##0.00` */
        val CURRENCY: CellStyle = PredefinedStyle.CURRENCY

        /** Integer formatted number: `#,##0` */
        val INTEGER: CellStyle = PredefinedStyle.INTEGER

        /** ISO Date formatted: `yyyy-mm-dd` */
        val DATE: CellStyle = PredefinedStyle.DATE

        /** Percentage formatted: `0.00%` */
        val PERCENTAGE: CellStyle = PredefinedStyle.PERCENTAGE

        /** Income: Green bold text (#1B5E20) with currency formatting */
        val INCOME: CellStyle = PredefinedStyle.INCOME

        /** Expense: Red bold text (#B71C1C) with currency formatting */
        val EXPENSE: CellStyle = PredefinedStyle.EXPENSE

        /** General bold text without background */
        val BOLD: CellStyle = PredefinedStyle.BOLD
    }
}

/**
 * Pre-baked cell styles mapping to initial indices (0-9) in styles.xml.
 */
enum class PredefinedStyle(override val index: Int) : CellStyle {
    DEFAULT(0),
    HEADER(1),
    SUBHEADER(2),
    CURRENCY(3),
    INTEGER(4),
    DATE(5),
    PERCENTAGE(6),
    INCOME(7),
    EXPENSE(8),
    BOLD(9)
}

/**
 * Custom cell style definition supporting dynamic colors and number formats.
 */
class CustomCellStyle internal constructor(
    override val index: Int,
    val backgroundColor: String? = null,
    val textColor: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val numberFormat: String? = null,
    val centered: Boolean = false
) : CellStyle

/**
 * Builder scope for configuring custom cell styles.
 */
class CellStyleBuilder {
    /** Background fill color in hex RGB format, e.g. "#1E88E5" or "1E88E5" */
    var backgroundColor: String? = null

    /** Text font color in hex RGB format, e.g. "#FFFFFF" or "FFFFFF" */
    var textColor: String? = null

    /** Whether the font is bold */
    var bold: Boolean = false

    /** Whether the font is italic */
    var italic: Boolean = false

    /** Custom Excel number format string, e.g. "$#,##0.00" or "dd/MM/yyyy" */
    var numberFormat: String? = null

    /** Whether text is horizontally centered */
    var centered: Boolean = false

    internal fun build(index: Int): CustomCellStyle {
        return CustomCellStyle(
            index = index,
            backgroundColor = backgroundColor?.trim()?.removePrefix("#")?.uppercase(),
            textColor = textColor?.trim()?.removePrefix("#")?.uppercase(),
            bold = bold,
            italic = italic,
            numberFormat = numberFormat,
            centered = centered
        )
    }
}
