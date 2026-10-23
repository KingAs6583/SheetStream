package com.fidluc.sheetstream

import java.io.File

/**
 * Runnable entry point to test and visually verify generated Excel spreadsheets.
 * Run this directly in Android Studio by clicking the green Play button.
 */
fun main() {
    val outputFile = File("sample_financial_report.xlsx")
    println("==================================================")
    println("SheetStream: High-Performance Excel Generator & Reader")
    println("==================================================")
    println("[1/2] Generating sample financial workbook...")

    outputFile.sheetStream {
        // Register custom brand accent styles
        val brandHeaderStyle = customStyle {
            backgroundColor = "1E3A8A" // Deep navy blue
            textColor = "FFFFFF"       // White text
            bold = true
            centered = true
        }

        val highlightStyle = customStyle {
            backgroundColor = "FEF3C7" // Warm amber highlight
            textColor = "92400E"       // Amber text
            bold = true
        }

        // ==========================================
        // SHEET 1: TRANSACTIONS (Multi-row ledger)
        // ==========================================
        sheet("Transactions") {
            // Set custom column widths (Date, Category, Description, Amount)
            setColumnWidths(15.0, 22.0, 32.0, 18.0)

            // Keep header row visible on vertical scroll
            freezeHeaderRow()

            // Header Row with brand style
            row {
                cell("Date", brandHeaderStyle)
                cell("Category", brandHeaderStyle)
                cell("Description", brandHeaderStyle)
                cell("Amount", brandHeaderStyle)
            }

            // Sample transaction rows (mimicking Room DB / Cursor export)
            val dummyData = listOf(
                Triple("2026-10-01", "Salary", 4500.00),
                Triple("2026-10-02", "Rent & Utilities", -1200.00),
                Triple("2026-10-05", "Groceries", -145.75),
                Triple("2026-10-08", "Dining Out", -65.20),
                Triple("2026-10-12", "Freelance Gig", 850.00),
                Triple("2026-10-15", "Internet & Mobile", -95.00),
                Triple("2026-10-18", "Investments", -500.00),
                Triple("2026-10-22", "Dividends", 125.40)
            )

            // Stream items row-by-row
            rows(dummyData) { (date, cat, amount) ->
                cell(date, CellStyle.DATE)
                cell(cat)
                cell("Bank Transfer / Card")
                val style = if (amount >= 0) CellStyle.INCOME else CellStyle.EXPENSE
                cell(amount, style)
            }
        }

        // ==========================================
        // SHEET 2: SUMMARY (Cross-sheet calculations)
        // ==========================================
        sheet("Summary") {
            setColumnWidths(25.0, 20.0)
            freezeHeaderRow()

            row {
                cell("Financial Metric", highlightStyle)
                cell("Value", highlightStyle)
            }

            row {
                cell("Total Transactions", CellStyle.BOLD)
                formula("COUNTA('Transactions'!A2:A9)", CellStyle.INTEGER)
            }

            row {
                cell("Net Cash Flow", CellStyle.BOLD)
                formula("SUM('Transactions'!D2:D9)", CellStyle.CURRENCY)
            }

            row {
                cell("Latest Transaction Ref", CellStyle.BOLD)
                formula("'Transactions'!B9")
            }
        }
    }

    println("[OK] File successfully generated at: ${outputFile.absolutePath}")
    println("File size: ${outputFile.length()} bytes")
    println()

    // ==========================================
    // READ BACK THE GENERATED FILE
    // ==========================================
    println("[2/2] Reading back the generated file using streaming SAX parser...")
    outputFile.readSpreadsheet {
        println("Discovered sheets in workbook: ${sheetNames.joinToString(", ")}")
        println()

        forEachSheet { sheetName ->
            println("--- Sheet: $sheetName ---")
            forEachRow { row ->
                val cellsFormatted = (1..row.columnCount).joinToString(" | ") { colIndex ->
                    val value = row.getString(colIndex) ?: ""
                    val formula = row.getFormula(colIndex)
                    if (formula != null) "$value [Formula: =$formula]" else value
                }
                println("Row ${row.rowNumber.toString().padStart(2, ' ')}: $cellsFormatted")
            }
            println()
        }
    }

    println("[OK] Demonstration complete. Generated file is ready for Excel / Google Sheets.")
}
