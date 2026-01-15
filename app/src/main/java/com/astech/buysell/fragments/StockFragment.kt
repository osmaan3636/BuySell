package com.astech.buysell.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.astech.buysell.R
import com.astech.buysell.adapters.ProductAdapter
import com.astech.buysell.databinding.FragmentStockBinding
import com.astech.buysell.viewmodel.StockViewModel
import com.astech.buysell.viewmodel.CartViewModel

/**
 * Fragment displaying all products in stock
 */
class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: StockViewModel by viewModels()
    private lateinit var productAdapter: ProductAdapter

    // Use activityViewModels for shared CartViewModel
    private val cartViewModel: CartViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter { product ->
            // Show Bottom Sheet on Click
            val bottomSheet = ProductSelectionBottomSheet(product)
            bottomSheet.show(parentFragmentManager, "ProductSelectionBottomSheet")
        }
        
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = productAdapter
            // Disable nested scrolling since it's inside a NestedScrollView
            isNestedScrollingEnabled = false // Important for NestedScrollView
        }
    }

    private fun setupClickListeners() {
        binding.btnSort.setOnClickListener {
            val isFiltering = viewModel.toggleLowStockFilter()
            val message = if (isFiltering) "Showing low stock items" else "Showing all items"
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
        
        // Open Search
        binding.btnSearch.setOnClickListener {
            showSearch(true)
        }
        
        // Close Search
        binding.btnCloseSearch.setOnClickListener {
            binding.etSearch.text.clear()
            showSearch(false)
            viewModel.searchProducts("")
        }
        
        // Search Text Watcher
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchProducts(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Go to Cart
        binding.fabCart.setOnClickListener {
            // Switch to Sell Tab (using ID from MainActivity if standard)
            // Or use an interface. For simplicity assuming direct access to BottomNav view in Activity layout
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_sell
        }
        
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshProducts()
        }
    }
    
    private fun showSearch(show: Boolean) {
        if (show) {
            binding.searchContainer.visibility = View.VISIBLE
            binding.tvHeaderTitle.visibility = View.INVISIBLE
            binding.btnSearch.visibility = View.INVISIBLE
            
            // Focus and show keyboard
            binding.etSearch.requestFocus()
            val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.showSoftInput(binding.etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.searchContainer.visibility = View.GONE
            binding.tvHeaderTitle.visibility = View.VISIBLE
            binding.btnSearch.visibility = View.VISIBLE
            
            // Hide keyboard
            val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    private fun setupObservers() {
        // Observe products list
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
            
            // Calculate total stock value
            val totalStockValue = products.sumOf { (it.buyPrice * it.stock) }
            val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("bn", "BD")).format(totalStockValue)
            binding.tvTotalBalance.text = "৳ $formattedTotal"
            
            // Show/hide empty state
            if (products.isEmpty() && viewModel.isLoading.value == false) {
                binding.emptyState.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyState.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
            }
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = false
            
            if (isLoading) {
                // Show progress only on initial load, not on refresh
                if (productAdapter.itemCount == 0) {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyState.visibility = View.GONE
                }
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }
        
        // Observe errors
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
