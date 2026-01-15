package com.astech.buysell.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.astech.buysell.models.CartItem
import com.astech.buysell.models.Product

/**
 * ViewModel shared between fragments to manage the shopping cart.
 */
class CartViewModel : ViewModel() {

    private val _cartItems = MutableLiveData<List<CartItem>>(emptyList())
    val cartItems: LiveData<List<CartItem>> = _cartItems

    private val _totalPrice = MutableLiveData<Double>(0.0)
    val totalPrice: LiveData<Double> = _totalPrice

    /**
     * Add or update product in cart
     */
    fun addToCart(product: Product, quantity: Int) {
        val currentList = _cartItems.value?.toMutableList() ?: mutableListOf()
        val existingItem = currentList.find { it.product.id == product.id }

        if (existingItem != null) {
            // Update quantity
            existingItem.quantity = quantity
            // If 0, remove it
            if (existingItem.quantity <= 0) {
                currentList.remove(existingItem)
            }
        } else {
            // Add new if quantity > 0
            if (quantity > 0) {
                currentList.add(CartItem(product, quantity))
            }
        }

        _cartItems.value = currentList
        calculateTotal()
    }

    /**
     * Remove product from cart
     */
    fun removeFromCart(product: Product) {
        val currentList = _cartItems.value?.toMutableList() ?: return
        currentList.removeAll { it.product.id == product.id }
        _cartItems.value = currentList
        calculateTotal()
    }

    /**
     * Clear the cart
     */
    fun clearCart() {
        _cartItems.value = emptyList()
        calculateTotal()
    }

    private fun calculateTotal() {
        val total = _cartItems.value?.sumOf { it.totalPrice } ?: 0.0
        _totalPrice.value = total
    }
}
