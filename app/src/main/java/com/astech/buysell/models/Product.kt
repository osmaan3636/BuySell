package com.astech.buysell.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data model representing a product in the inventory.
 * Uses Kotlinx Serialization for Supabase compatibility.
 */
@Serializable
data class Product(
    @SerialName("id")
    val id: String? = null,
    
    @SerialName("name")
    val name: String = "",
    
    @SerialName("buy_price")
    val buyPrice: Double = 0.0,
    
    @SerialName("sell_price")
    val sellPrice: Double = 0.0,
    
    @SerialName("stock")
    val stock: Int = 0,
    
    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("image_url")
    val imageUrl: String? = null
) {
    /**
     * Format buy price for display with currency
     */
    fun formattedBuyPrice(): String = "${buyPrice.toInt()} TK"
    
    /**
     * Format sell price for display with currency
     */
    fun formattedSellPrice(): String = "${sellPrice.toInt()} TK"
    
    /**
     * Check if product has sufficient stock
     */
    fun hasStock(quantity: Int): Boolean = stock >= quantity
    
    /**
     * Calculate potential profit per unit
     */
    fun profitPerUnit(): Double = sellPrice - buyPrice
}
