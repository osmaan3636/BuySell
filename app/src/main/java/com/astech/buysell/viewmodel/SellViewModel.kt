package com.astech.buysell.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astech.buysell.models.DiscountType
import com.astech.buysell.models.Product
import com.astech.buysell.models.SellTransaction
import com.astech.buysell.repository.ProductRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Sell feature.
 * Handles business logic for discount calculations, validation, and Supabase operations.
 */
class SellViewModel : ViewModel() {
    
    private val repository = ProductRepository()
    
    // Product being sold
    private val _product = MutableLiveData<Product?>()
    val product: LiveData<Product?> = _product
    
    // User inputs
    private val _quantity = MutableLiveData(1)
    val quantity: LiveData<Int> = _quantity
    
    private val _discountType = MutableLiveData(DiscountType.NONE)
    val discountType: LiveData<DiscountType> = _discountType
    
    private val _discountValue = MutableLiveData(0.0)
    val discountValue: LiveData<Double> = _discountValue
    
    // Calculated values
    private val _finalPrice = MutableLiveData(0.0)
    val finalPrice: LiveData<Double> = _finalPrice
    
    private val _totalProfit = MutableLiveData(0.0)
    val totalProfit: LiveData<Double> = _totalProfit
    
    private val _discountAmount = MutableLiveData(0.0)
    val discountAmount: LiveData<Double> = _discountAmount
    
    // UI state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    /**
     * Load product by ID from Supabase
     */
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val productData = repository.getProductById(productId)
            if (productData != null) {
                _product.value = productData
                recalculate()
            } else {
                _errorMessage.value = "Product not found"
            }
            _isLoading.value = false
        }
    }
    
    /**
     * Set the product directly (useful for testing or when passing product object)
     */
    fun setProduct(product: Product) {
        _product.value = product
        recalculate()
    }
    
    /**
     * Update quantity
     */
    fun setQuantity(qty: Int) {
        if (qty > 0) {
            _quantity.value = qty
            recalculate()
        }
    }
    
    /**
     * Update discount type (clears opposite discount value for mutual exclusivity)
     */
    fun setDiscountType(type: DiscountType) {
        _discountType.value = type
        if (type == DiscountType.NONE) {
            _discountValue.value = 0.0
        }
        recalculate()
    }
    
    /**
     * Set percentage discount (0-100)
     */
    fun setPercentageDiscount(percentage: Double) {
        if (percentage in 0.0..100.0) {
            _discountType.value = DiscountType.PERCENTAGE
            _discountValue.value = percentage
            _errorMessage.value = null
            recalculate()
        } else {
            _errorMessage.value = "Percentage must be between 0 and 100"
        }
    }
    
    /**
     * Set direct final price
     */
    fun setDirectPrice(price: Double) {
        if (price > 0) {
            _discountType.value = DiscountType.DIRECT_PRICE
            _discountValue.value = price
            _errorMessage.value = null
            recalculate()
        } else {
            _errorMessage.value = "Price must be greater than 0"
        }
    }
    
    /**
     * Clear discount and reset to no discount
     */
    fun clearDiscount() {
        _discountType.value = DiscountType.NONE
        _discountValue.value = 0.0
        recalculate()
    }
    
    /**
     * Recalculate final price and profit based on current inputs
     */
    private fun recalculate() {
        val productData = _product.value ?: return
        val qty = _quantity.value ?: 1
        val type = _discountType.value ?: DiscountType.NONE
        val value = _discountValue.value ?: 0.0
        
        // Calculate final price based on discount type
        val calculatedFinalPrice = SellTransaction.calculateFinalPrice(
            originalPrice = productData.sellPrice,
            discountType = type,
            discountValue = value
        )
        
        _finalPrice.value = calculatedFinalPrice
        
        // Calculate discount amount
        _discountAmount.value = productData.sellPrice - calculatedFinalPrice
        
        // Calculate total profit
        val profit = SellTransaction.calculateTotalProfit(
            finalPrice = calculatedFinalPrice,
            buyPrice = productData.buyPrice,
            quantity = qty
        )
        
        _totalProfit.value = profit
    }
    
    /**
     * Validate all inputs before executing sale
     * @return true if valid, false otherwise (sets error message)
     */
    fun validateInputs(): Boolean {
        val productData = _product.value
        if (productData == null) {
            _errorMessage.value = "No product selected"
            return false
        }
        
        val qty = _quantity.value ?: 0
        if (qty <= 0) {
            _errorMessage.value = "Quantity must be at least 1"
            return false
        }
        
        if (!productData.hasStock(qty)) {
            _errorMessage.value = "Insufficient stock. Available: ${productData.stock}"
            return false
        }
        
        val type = _discountType.value ?: DiscountType.NONE
        val value = _discountValue.value ?: 0.0
        
        if (type == DiscountType.PERCENTAGE && (value < 0 || value > 100)) {
            _errorMessage.value = "Percentage must be between 0 and 100"
            return false
        }
        
        if (type == DiscountType.DIRECT_PRICE && value <= 0) {
            _errorMessage.value = "Final price must be greater than 0"
            return false
        }
        
        val calculatedPrice = _finalPrice.value ?: 0.0
        if (calculatedPrice <= 0) {
            _errorMessage.value = "Invalid final price"
            return false
        }
        
        // All validations passed
        _errorMessage.value = null
        return true
    }
    
    /**
     * Execute the sell transaction
     * Saves to Supabase and reduces stock
     */
    fun executeSell() {
        if (!validateInputs()) {
            return
        }
        
        val productData = _product.value!!
        val qty = _quantity.value!!
        val type = _discountType.value!!
        val value = _discountValue.value!!
        val calculatedFinalPrice = _finalPrice.value!!
        val profit = _totalProfit.value!!
        
        // Create transaction object
        val transaction = SellTransaction(
            productId = productData.id ?: "",
            productName = productData.name,
            originalSellPrice = productData.sellPrice,
            discountType = type.name,
            discountValue = value,
            finalPrice = calculatedFinalPrice,
            quantity = qty,
            buyPrice = productData.buyPrice,
            totalProfit = profit
        )
        
        // Execute in coroutine
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val success = repository.executeSellTransaction(transaction)
            
            _isLoading.value = false
            
            if (success) {
                _successMessage.value = "Product sold successfully!"
            } else {
                _errorMessage.value = "Error saving transaction. Please try again."
            }
        }
    }
    
    /**
     * Clear success message (call after showing to user)
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Clear error message (call after showing to user)
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
