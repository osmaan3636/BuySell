package com.astech.buysell.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astech.buysell.models.Product
import com.astech.buysell.repository.ProductRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Stock page data and state
 */
class StockViewModel : ViewModel() {
    
    private val repository = ProductRepository()
    
    private val _products = MutableLiveData<List<Product>>()
    val products: LiveData<List<Product>> = _products
    
    // Cache original list for searching
    private var _allProducts: List<Product> = emptyList()
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        loadProducts()
    }
    
    /**
     * Load all products from Supabase
     */
    fun loadProducts() {
        _isLoading.value = true
        _errorMessage.value = null
        
        viewModelScope.launch {
            try {
                val productList = repository.getAllProducts()
                _allProducts = productList
                _products.value = productList
                
                if (productList.isEmpty()) {
                    // Don't set error for empty list, let UI handle empty state
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Filter products based on search query
     */
    private var _currentQuery = ""
    private var _isLowStockFilterActive = false
    
    /**
     * Filter products based on search query
     */
    fun searchProducts(query: String) {
        _currentQuery = query
        applyFilters()
    }

    /**
     * Toggle "Low Stock" filter
     * Returns the new state (true = filtering low stock, false = showing all)
     */
    fun toggleLowStockFilter(): Boolean {
        _isLowStockFilterActive = !_isLowStockFilterActive
        applyFilters()
        return _isLowStockFilterActive
    }

    private fun applyFilters() {
        var filteredList = _allProducts
        
        // Apply search filter
        if (_currentQuery.isNotBlank()) {
            filteredList = filteredList.filter { 
                it.name.contains(_currentQuery, ignoreCase = true) 
            }
        }
        
        // Apply low stock filter (stock <= 5)
        if (_isLowStockFilterActive) {
            filteredList = filteredList.filter { it.stock <= 5 }
        }
        
        _products.value = filteredList
    }
    
    /**
     * Refresh products (used by SwipeRefreshLayout)
     */
    fun refreshProducts() {
        loadProducts()
    }
    
    /**
     * Clear error message after showing to user
     */
    fun clearError() {
        _errorMessage.value = null
    }
}
