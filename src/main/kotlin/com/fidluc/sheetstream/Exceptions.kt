package com.fidluc.sheetstream

/**
 * Base exception for all SheetStream errors.
 */
open class SheetStreamException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Thrown when a spreadsheet exceeds defined memory, file size, or Excel specification limits.
 */
class SheetStreamLimitExceededException(
    val limitType: LimitType,
    val actualValue: Long,
    val maxValue: Long,
    message: String
) : SheetStreamException(message)

/**
 * Thrown when an input file is corrupted, not a valid .xlsx archive, or missing critical OOXML structures.
 */
class InvalidSpreadsheetException(
    message: String,
    cause: Throwable? = null
) : SheetStreamException(message, cause)

/**
 * Categories of safety and specification limits.
 */
enum class LimitType {
    /** Total file size in bytes */
    FILE_SIZE,

    /** Number of unique text entries in xl/sharedStrings.xml */
    SHARED_STRINGS,

    /** Number of rows in a single worksheet (Excel limit: 1,048,576) */
    ROW_COUNT,

    /** Number of columns in a single row (Excel limit: 16,384 / Column XFD) */
    COLUMN_COUNT
}

/**
 * Configurable safety limits for reading spreadsheets on mobile devices.
 */
data class ReaderConfig(
    /** Maximum allowed file size in bytes (default 50 MB) */
    val maxFileSizeBytes: Long = DEFAULT_MAX_FILE_SIZE,

    /** Maximum allowed shared strings in memory (default 50,000) */
    val maxSharedStrings: Int = DEFAULT_MAX_SHARED_STRINGS,

    /** Maximum allowed rows to parse per sheet (default 1,048,576) */
    val maxRowsPerSheet: Int = DEFAULT_MAX_ROWS
) {
    companion object {
        const val DEFAULT_MAX_FILE_SIZE: Long = 50L * 1024L * 1024L // 50 MB
        const val DEFAULT_MAX_SHARED_STRINGS: Int = 50_000
        const val DEFAULT_MAX_ROWS: Int = 1_048_576

        /** Unconstrained configuration for high-memory server environments */
        val UNLIMITED = ReaderConfig(
            maxFileSizeBytes = Long.MAX_VALUE,
            maxSharedStrings = Int.MAX_VALUE,
            maxRowsPerSheet = Int.MAX_VALUE
        )
    }
}
