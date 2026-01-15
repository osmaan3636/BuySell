package com.astech.buysell.models

import kotlinx.serialization.Serializable

/**
 * Data class representing an item in the shopping cart.
 */
@Serializable
data class CartItem(
    val product: Product,
    var quantity: Int
) {
    val totalPrice: Double
        get() = product.sellPrice * quantity
}
