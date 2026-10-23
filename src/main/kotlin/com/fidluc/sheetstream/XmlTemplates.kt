package com.fidluc.sheetstream

internal object XmlTemplates {

    fun escapeXml(text: String): String {
        val out = StringBuilder(text.length + 16)
        for (ch in text) {
            when (ch) {
                '&' -> out.append("&amp;")
                '<' -> out.append("&lt;")
                '>' -> out.append("&gt;")
                '"' -> out.append("&quot;")
                '\'' -> out.append("&apos;")
                else -> {
                    // Filter out invalid XML control characters that cause Excel corruption
                    if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch)
                    }
                }
            }
        }
        return out.toString()
    }

    fun columnLetter(colIndex1Based: Int): String {
        var temp = colIndex1Based
        val result = StringBuilder()
        while (temp > 0) {
            temp--
            result.insert(0, ('A'.code + (temp % 26)).toChar())
            temp /= 26
        }
        return result.toString()
    }

    fun buildContentTypes(sheetCount: Int): String {
        val sheets = (1..sheetCount).joinToString("") { i ->
            """<Override PartName="/xl/worksheets/sheet$i.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
$sheets
</Types>""".trimIndent()
    }

    const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    fun buildWorkbookRels(sheetCount: Int): String {
        val sheetRels = (1..sheetCount).joinToString("") { i ->
            """<Relationship Id="rId$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$i.xml"/>"""
        }
        val stylesRelId = "rId${sheetCount + 1}"
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$sheetRels
<Relationship Id="$stylesRelId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".trimIndent()
    }

    fun buildWorkbook(sheetNames: List<String>): String {
        val sheetsXml = sheetNames.mapIndexed { index, name ->
            val sheetId = index + 1
            val rId = "rId$sheetId"
            """<sheet name="${escapeXml(name)}" sheetId="$sheetId" r:id="$rId"/>"""
        }.joinToString("")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>
$sheetsXml
</sheets>
</workbook>""".trimIndent()
    }

    /**
     * Pre-compiled styles table:
     * Style 0: Default
     * Style 1: Header (Dark Green #1E4620, White bold text, centered)
     * Style 2: Subheader (Light Gray #EEEEEE, Black bold text)
     * Style 3: Currency (#,##0.00)
     * Style 4: Integer (#,##0)
     * Style 5: Date (yyyy-mm-dd)
     * Style 6: Percentage (0.00%)
     * Style 7: Income (Green text #1B5E20, Bold, Currency format)
     * Style 8: Expense (Red text #B71C1C, Bold, Currency format)
     * Style 9: Bold Text (General bold)
     */
    fun buildStylesXml(customStyles: List<CustomCellStyle> = emptyList()): String {
        // Dynamic Number Formats
        val customNumFmts = mutableListOf<String>()
        val styleNumFmtMap = mutableMapOf<Int, Int>() // styleIndex -> numFmtId
        var nextNumFmtId = 170

        customStyles.forEach { style ->
            if (!style.numberFormat.isNullOrBlank()) {
                val id = nextNumFmtId++
                styleNumFmtMap[style.index] = id
                customNumFmts.add("""  <numFmt numFmtId="$id" formatCode="${escapeXml(style.numberFormat)}"/>""")
            }
        }

        val totalNumFmts = 3 + customNumFmts.size
        val numFmtsXml = """<numFmts count="$totalNumFmts">
  <numFmt numFmtId="164" formatCode="#,##0.00"/>
  <numFmt numFmtId="165" formatCode="yyyy-mm-dd"/>
  <numFmt numFmtId="166" formatCode="+#,##0.00;-#,##0.00;0.00"/>
${customNumFmts.joinToString("\n")}
</numFmts>"""

        // Dynamic Fonts
        val customFonts = mutableListOf<String>()
        val styleFontMap = mutableMapOf<Int, Int>() // styleIndex -> fontId
        var nextFontId = 6

        customStyles.forEach { style ->
            val hasCustomFont = style.bold || style.italic || style.textColor != null
            if (hasCustomFont) {
                val fId = nextFontId++
                styleFontMap[style.index] = fId
                val boldXml = if (style.bold) "<b/>" else ""
                val italicXml = if (style.italic) "<i/>" else ""
                val colorXml = if (style.textColor != null) """<color rgb="FF${style.textColor}"/>""" else ""
                customFonts.add("""  <font>$boldXml$italicXml<sz val="11"/>$colorXml<name val="Calibri"/></font>""")
            } else {
                styleFontMap[style.index] = 0 // default regular font
            }
        }

        val totalFonts = 6 + customFonts.size
        val fontsXml = """<fonts count="$totalFonts">
  <!-- 0: Regular -->
  <font><sz val="11"/><name val="Calibri"/></font>
  <!-- 1: White Bold (Header) -->
  <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  <!-- 2: Dark Bold (Subheader / General Bold) -->
  <font><b/><sz val="11"/><name val="Calibri"/></font>
  <!-- 3: Green Bold (Income) -->
  <font><b/><sz val="11"/><color rgb="FF1B5E20"/><name val="Calibri"/></font>
  <!-- 4: Red Bold (Expense) -->
  <font><b/><sz val="11"/><color rgb="FFB71C1C"/><name val="Calibri"/></font>
  <!-- 5: Italic Subtle -->
  <font><i/><sz val="10"/><color rgb="FF757575"/><name val="Calibri"/></font>
${customFonts.joinToString("\n")}
</fonts>"""

        // Dynamic Fills
        val customFills = mutableListOf<String>()
        val styleFillMap = mutableMapOf<Int, Int>() // styleIndex -> fillId
        var nextFillId = 4

        customStyles.forEach { style ->
            if (style.backgroundColor != null) {
                val fId = nextFillId++
                styleFillMap[style.index] = fId
                customFills.add("""  <fill><patternFill patternType="solid"><fgColor rgb="FF${style.backgroundColor}"/></patternFill></fill>""")
            } else {
                styleFillMap[style.index] = 0 // no fill
            }
        }

        val totalFills = 4 + customFills.size
        val fillsXml = """<fills count="$totalFills">
  <!-- 0: None -->
  <fill><patternFill patternType="none"/></fill>
  <!-- 1: Gray125 (reserved by Excel) -->
  <fill><patternFill patternType="gray125"/></fill>
  <!-- 2: Dark Green Fill (#1E4620) -->
  <fill><patternFill patternType="solid"><fgColor rgb="FF1E4620"/></patternFill></fill>
  <!-- 3: Light Gray Fill (#EEEEEE) -->
  <fill><patternFill patternType="solid"><fgColor rgb="FFEEEEEE"/></patternFill></fill>
${customFills.joinToString("\n")}
</fills>"""

        // Dynamic CellXfs
        val customXfs = customStyles.map { style ->
            val numFmtId = styleNumFmtMap[style.index] ?: 0
            val fontId = styleFontMap[style.index] ?: 0
            val fillId = styleFillMap[style.index] ?: 0
            val applyFont = if (fontId != 0) " applyFont=\"1\"" else ""
            val applyFill = if (fillId != 0) " applyFill=\"1\"" else ""
            val applyNumFmt = if (numFmtId != 0) " applyNumberFormat=\"1\"" else ""
            val alignment = if (style.centered) """ applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>""" else "/>"

            if (style.centered) {
                """  <xf numFmtId="$numFmtId" fontId="$fontId" fillId="$fillId" borderId="1" xfId="0"$applyNumFmt$applyFont$applyFill applyBorder="1"$alignment"""
            } else {
                """  <xf numFmtId="$numFmtId" fontId="$fontId" fillId="$fillId" borderId="1" xfId="0"$applyNumFmt$applyFont$applyFill applyBorder="1"/>"""
            }
        }

        val totalCellXfs = 10 + customXfs.size
        val cellXfsXml = """<cellXfs count="$totalCellXfs">
  <!-- 0: Default -->
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
  <!-- 1: Header (White text, Green BG, Centered, Thin border) -->
  <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
    <alignment horizontal="center" vertical="center"/>
  </xf>
  <!-- 2: Subheader (Dark Bold, Gray BG, Thin border) -->
  <xf numFmtId="0" fontId="2" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
  <!-- 3: Currency (#,##0.00) -->
  <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
  <!-- 4: Integer (#,##0) -->
  <xf numFmtId="3" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
  <!-- 5: Date (yyyy-mm-dd) -->
  <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
  <!-- 6: Percentage (0.00%) -->
  <xf numFmtId="10" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
  <!-- 7: Income (Green Bold + Currency) -->
  <xf numFmtId="166" fontId="3" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1"/>
  <!-- 8: Expense (Red Bold + Currency) -->
  <xf numFmtId="164" fontId="4" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyBorder="1"/>
  <!-- 9: General Bold -->
  <xf numFmtId="0" fontId="2" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1"/>
${customXfs.joinToString("\n")}
</cellXfs>"""

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
$numFmtsXml
$fontsXml
$fillsXml
<borders count="2">
  <!-- 0: None -->
  <border><left/><right/><top/><bottom/><diagonal/></border>
  <!-- 1: Thin border -->
  <border>
    <left style="thin"><color rgb="FFD0D0D0"/></left>
    <right style="thin"><color rgb="FFD0D0D0"/></right>
    <top style="thin"><color rgb="FFD0D0D0"/></top>
    <bottom style="thin"><color rgb="FFD0D0D0"/></bottom>
  </border>
</borders>
<cellStyleXfs count="1">
  <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
</cellStyleXfs>
$cellXfsXml
</styleSheet>""".trimIndent()
    }
}

