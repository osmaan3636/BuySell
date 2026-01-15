package com.astech.buysell

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.astech.buysell.databinding.ActivitySellBinding
import com.astech.buysell.models.DiscountType
import com.astech.buysell.models.Product
import com.astech.buysell.viewmodel.SellViewModel
import com.google.android.material.snackbar.Snackbar

/**
 * Activity for selling products with discount functionality.
 * Supports percentage discount and direct final price input.
 */
class SellActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySellBinding
    private val viewModel: SellViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySellBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up action bar
        supportActionBar?.title = getString(R.string.activity_sell)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Initialize with sample product for demonstration
        // In production, you would get productId from intent and load from Supabase
        initializeSampleProduct()
        
        setupObservers()
        setupListeners()
    }
    
    /**
     * Initialize with a sample product for demonstration purposes.
     * Replace this with actual product loading from intent extras.
     */
    private fun initializeSampleProduct() {
        // TODO: In production, get product ID from intent:
        // val productId = intent.getStringExtra("PRODUCT_ID") ?: return
        // viewModel.loadProduct(productId)
        
        // Sample product for demonstration
        val sampleProduct = Product(
            id = "sample-id",
            name = "Sample Product",
            buyPrice = 800.0,
            sellPrice = 1500.0,
            stock = 25
        )
        viewModel.setProduct(sampleProduct)
    }
    
    /**
     * Set up LiveData observers for reactive UI updates
     */
    private fun setupObservers() {
        // Observe product
        viewModel.product.observe(this) { product ->
            product?.let {
                displayProductInfo(it)
            }
        }
        
        // Observe final price (real-time calculation)
        viewModel.finalPrice.observe(this) { finalPrice ->
            binding.tvFinalPrice.text = getString(R.string.format_price, finalPrice.toInt().toString())
            binding.tvOriginalPriceSummary.text = viewModel.product.value?.formattedSellPrice() ?: ""
        }
        
        // Observe total profit
        viewModel.totalProfit.observe(this) { profit ->
            binding.tvProfit.text = getString(R.string.format_price, profit.toInt().toString())
            
            // Change color based on profit (green if positive, red if negative)
            binding.tvProfit.setTextColor(
                if (profit >= 0) getColor(R.color.profit_color)
                else getColor(R.color.error_red)
            )
        }
        
        // Observe discount amount
        viewModel.discountAmount.observe(this) { discountAmount ->
            if (discountAmount > 0) {
                binding.layoutDiscountDisplay.visibility = View.VISIBLE
                val product = viewModel.product.value
                val discountPercentage = if (product != null && product.sellPrice > 0) {
                    (discountAmount / product.sellPrice) * 100.0
                } else 0.0
                
                binding.tvDiscountAmount.text = getString(
                    R.string.format_discount,
                    discountAmount.toInt().toString(),
                    discountPercentage
                )
            } else {
                binding.layoutDiscountDisplay.visibility = View.GONE
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSell.isEnabled = !isLoading
            binding.btnCancel.isEnabled = !isLoading
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it)
                viewModel.clearErrorMessage()
            }
        }
        
        // Observe success messages
        viewModel.successMessage.observe(this) { successMessage ->
            successMessage?.let {
                showSuccess(it)
                viewModel.clearSuccessMessage()
                // Finish activity on success
                finish()
            }
        }
    }
    
    /**
     * Display product information in the UI
     */
    private fun displayProductInfo(product: Product) {
        binding.tvProductName.text = product.name
        binding.tvBuyPrice.text = product.formattedBuyPrice()
        binding.tvOriginalPrice.text = product.formattedSellPrice()
        binding.tvAvailableStock.text = getString(R.string.label_available_stock, product.stock)
    }
    
    /**
     * Set up all UI listeners
     */
    private fun setupListeners() {
        // Quantity input listener
        binding.etQuantity.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val quantity = s?.toString()?.toIntOrNull() ?: 1
                viewModel.setQuantity(if (quantity > 0) quantity else 1)
            }
        })
        
        // Radio group listener for discount type
        binding.radioGroupDiscount.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioNoDiscount -> {
                    binding.layoutPercentage.visibility = View.GONE
                    binding.layoutDirectPrice.visibility = View.GONE
                    binding.etPercentage.text?.clear()
                    binding.etDirectPrice.text?.clear()
                    viewModel.clearDiscount()
                }
                R.id.radioPercentage -> {
                    binding.layoutPercentage.visibility = View.VISIBLE
                    binding.layoutDirectPrice.visibility = View.GONE
                    binding.etDirectPrice.text?.clear()
                    viewModel.setDiscountType(DiscountType.PERCENTAGE)
                }
                R.id.radioDirectPrice -> {
                    binding.layoutPercentage.visibility = View.GONE
                    binding.layoutDirectPrice.visibility = View.VISIBLE
                    binding.etPercentage.text?.clear()
                    viewModel.setDiscountType(DiscountType.DIRECT_PRICE)
                }
            }
        }
        
        // Percentage input listener
        binding.etPercentage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val percentage = s?.toString()?.toDoubleOrNull()
                if (percentage != null) {
                    viewModel.setPercentageDiscount(percentage)
                    binding.layoutPercentage.error = null
                } else if (s?.isNotEmpty() == true) {
                    binding.layoutPercentage.error = getString(R.string.error_percentage_range)
                }
            }
        })
        
        // Direct price input listener
        binding.etDirectPrice.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val price = s?.toString()?.toDoubleOrNull()
                if (price != null && price > 0) {
                    viewModel.setDirectPrice(price)
                    binding.layoutDirectPrice.error = null
                } else if (s?.isNotEmpty() == true) {
                    binding.layoutDirectPrice.error = getString(R.string.error_invalid_price)
                }
            }
        })
        
        // Sell button listener
        binding.btnSell.setOnClickListener {
            if (viewModel.validateInputs()) {
                showConfirmationDialog()
            }
        }
        
        // Cancel button listener
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    /**
     * Show confirmation dialog before executing sale
     */
    private fun showConfirmationDialog() {
        val product = viewModel.product.value ?: return
        val quantity = viewModel.quantity.value ?: 1
        val finalPrice = viewModel.finalPrice.value ?: 0.0
        val profit = viewModel.totalProfit.value ?: 0.0
        
        val message = getString(
            R.string.dialog_message_confirm,
            quantity,
            product.name,
            finalPrice.toInt().toString(),
            profit.toInt().toString()
        )
        
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_confirm)
            .setMessage(message)
            .setPositiveButton(R.string.dialog_btn_confirm) { _, _ ->
                viewModel.executeSell()
            }
            .setNegativeButton(R.string.dialog_btn_cancel, null)
            .show()
    }
    
    /**
     * Show error message via Snackbar
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.error_red))
            .setTextColor(getColor(R.color.white))
            .show()
    }
    
    /**
     * Show success message via Snackbar
     */
    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(getColor(R.color.success_green))
            .setTextColor(getColor(R.color.white))
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
