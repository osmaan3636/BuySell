

package com.astech.buysell.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.astech.buysell.R
import com.astech.buysell.adapters.CartAdapter
import com.astech.buysell.databinding.FragmentSellBinding
import com.astech.buysell.models.DiscountType
import com.astech.buysell.models.SellTransaction
import com.astech.buysell.repository.ProductRepository
import com.astech.buysell.viewmodel.CartViewModel
import kotlinx.coroutines.launch

class SellFragment : Fragment() {

    private var _binding: FragmentSellBinding? = null
    private val binding get() = _binding!!
    
    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter
    private val repository = ProductRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSellBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupRecyclerView() {
        cartAdapter = CartAdapter { item ->
            cartViewModel.removeFromCart(item.product)
        }
        
        binding.recyclerViewCart.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cartAdapter
        }
    }

    private fun setupObservers() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            cartAdapter.submitList(items)
            
            if (items.isEmpty()) {
                binding.layoutEmptyCart.visibility = View.VISIBLE
                binding.cardCheckout.visibility = View.GONE
            } else {
                binding.layoutEmptyCart.visibility = View.GONE
                binding.cardCheckout.visibility = View.VISIBLE
            }
        }
        
        cartViewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            binding.tvTotalAmount.text = "${total.toInt()} TK"
        }
    }

    private fun setupListeners() {
        binding.btnGoToStock.setOnClickListener {
            requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_buy
        }
        
        binding.btnCheckout.setOnClickListener {
            showConfirmationDialog()
        }
    }
    
    private fun showConfirmationDialog() {
        val total = cartViewModel.totalPrice.value ?: 0.0
        val count = cartViewModel.cartItems.value?.size ?: 0
        
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Sale")
            .setMessage("Are you sure you want to sell $count items for ${total.toInt()} TK?")
            .setPositiveButton("Confirm") { _, _ ->
                executeCheckout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun executeCheckout() {
        val items = cartViewModel.cartItems.value ?: return
        if (items.isEmpty()) return
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCheckout.isEnabled = false
        
        viewLifecycleOwner.lifecycleScope.launch {
            var successCount = 0
            val errors = mutableListOf<String>()
            
            items.forEach { item ->
                val finalPrice = item.product.sellPrice * item.quantity
                val profit = (item.product.sellPrice - item.product.buyPrice) * item.quantity
                
                val transaction = SellTransaction(
                    productId = item.product.id ?: "",
                    productName = item.product.name,
                    originalSellPrice = item.product.sellPrice,
                    discountType = DiscountType.NONE.name,
                    finalPrice = finalPrice,
                    quantity = item.quantity,
                    buyPrice = item.product.buyPrice,
                    totalProfit = profit
                )
                
                val success = repository.executeSellTransaction(transaction)
                if (success) {
                    successCount++
                } else {
                    errors.add(item.product.name)
                }
            }
            
            binding.progressBar.visibility = View.GONE
            binding.btnCheckout.isEnabled = true
            
            if (errors.isEmpty()) {
                // All success
                Toast.makeText(context, "Sale Successful!", Toast.LENGTH_LONG).show()
                cartViewModel.clearCart()
                // Navigate to Reports or just stay? 
                // Let's go to Stock to sell more
                requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.nav_buy
            } else {
                Toast.makeText(context, "Completed with errors. Failed: ${errors.joinToString()}", Toast.LENGTH_LONG).show()
                // Refresh cart ? Keeping items that failed would be complex without ID matching logic
                // For now, allow user to clear or retry.
                cartViewModel.clearCart() // Clearing for simplicity to avoid stuck state
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
