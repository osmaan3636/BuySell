package com.astech.buysell.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data model representing a sell transaction with discount support.
 * This is saved to Supabase after each successful sale.
 */
@Serializable
data class SellTransaction(
    @SerialName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerialName("product_id")
    val productId: String = "",
    
    @SerialName("product_name")
    val productName: String = "",
    
    @SerialName("original_sell_price")
    val originalSellPrice: Double = 0.0,
    
    @SerialName("discount_type")
    val discountType: String = DiscountType.NONE.name,
    
    @SerialName("discount_value")
    val discountValue: Double = 0.0,
    
    @SerialName("final_price")
    val finalPrice: Double = 0.0,
    
    @SerialName("quantity")
    val quantity: Int = 0,
    
    @SerialName("buy_price")
    val buyPrice: Double = 0.0,
    
    @SerialName("total_profit")
    val totalProfit: Double = 0.0,
    
    @SerialName("created_at")
    val createdAt: String = getCurrentTimestamp()
) {
    companion object {
        /**
         * Get current timestamp in ISO 8601 format
         */
        private fun getCurrentTimestamp(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date())
        }
        
        /**
         * Calculate final price based on discount type
         */
        fun calculateFinalPrice(
            originalPrice: Double,
            discountType: DiscountType,
            discountValue: Double
        ): Double {
            return when (discountType) {
                DiscountType.PERCENTAGE -> {
                    originalPrice * (1 - discountValue / 100.0)
                }
                DiscountType.DIRECT_PRICE -> {
                    discountValue
                }
                DiscountType.NONE -> {
                    originalPrice
                }
            }
        }
        
        /**
         * Calculate total profit
         */
        fun calculateTotalProfit(
            finalPrice: Double,
            buyPrice: Double,
            quantity: Int
        ): Double {
            return (finalPrice - buyPrice) * quantity
        }
    }
    
    /**
     * Calculate discount amount in currency
     */
    fun discountAmount(): Double {
        return originalSellPrice - finalPrice
    }
    
    /**
     * Calculate discount percentage
     */
    fun discountPercentage(): Double {
        if (originalSellPrice == 0.0) return 0.0
        return ((originalSellPrice - finalPrice) / originalSellPrice) * 100.0
    }
    
    /**
     * Format final price for display
     */
    fun formattedFinalPrice(): String = "${finalPrice.toInt()} TK"
    
    /**
     * Format profit for display
     */
    fun formattedProfit(): String = "${totalProfit.toInt()} TK"
}
