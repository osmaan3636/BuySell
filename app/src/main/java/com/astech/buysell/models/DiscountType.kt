package com.astech.buysell.models

/**
 * Enum representing the type of discount applied to a sale
 */
enum class DiscountType {
    /** Percentage-based discount (e.g., 5% off) */
    PERCENTAGE,
    
    /** Direct final price specified by user */
    DIRECT_PRICE,
    
    /** No discount applied */
    NONE;
    
    companion object {
        /**
         * Convert string from database to enum
         */
        fun fromString(value: String): DiscountType {
            return when (value.uppercase()) {
                "PERCENTAGE" -> PERCENTAGE
                "DIRECT_PRICE" -> DIRECT_PRICE
                else -> NONE
            }
        }
    }
}
