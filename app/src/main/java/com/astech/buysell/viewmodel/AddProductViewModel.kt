package com.astech.buysell.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astech.buysell.models.Product
import com.astech.buysell.repository.ProductRepository
import kotlinx.coroutines.launch
import java.util.UUID

class AddProductViewModel : ViewModel() {
    
    private val repository = ProductRepository()
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun addProduct(name: String, buyPrice: String, sellPrice: String, stock: String, imageBytes: ByteArray? = null) {
        if (!validateInput(name, buyPrice, sellPrice, stock)) {
            return
        }
        
        _isLoading.value = true
        viewModelScope.launch {
            var imageUrl: String? = null
            
            // Upload image if provided
            if (imageBytes != null) {
                // Generate unique filename
                val fileName = "prod_${UUID.randomUUID()}.jpg"
                imageUrl = repository.uploadImage(imageBytes, fileName)
                
                if (imageUrl == null) {
                    _errorMessage.value = "Failed to upload image. Continuing without image."
                }
            }
            
            val product = Product(
                name = name,
                buyPrice = buyPrice.toDouble(),
                sellPrice = sellPrice.toDouble(),
                stock = stock.toInt(),
                imageUrl = imageUrl
            )
            
            val success = repository.addProduct(product)
            _isLoading.value = false
            
            if (success) {
                _successMessage.value = "Product added successfully!"
            } else {
                _errorMessage.value = "Failed to add product. Please try again."
            }
        }
    }
    
    private fun validateInput(name: String, buyPrice: String, sellPrice: String, stock: String): Boolean {
        if (name.isBlank()) {
            _errorMessage.value = "Product name is required"
            return false
        }
        
        val buy = buyPrice.toDoubleOrNull()
        if (buy == null || buy <= 0) {
            _errorMessage.value = "Invalid buy price"
            return false
        }
        
        val sell = sellPrice.toDoubleOrNull()
        if (sell == null || sell <= 0) {
            _errorMessage.value = "Invalid sell price"
            return false
        }
        
        val qty = stock.toIntOrNull()
        if (qty == null || qty < 0) {
            _errorMessage.value = "Invalid stock quantity"
            return false
        }
        
        return true
    }
    
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }
}
