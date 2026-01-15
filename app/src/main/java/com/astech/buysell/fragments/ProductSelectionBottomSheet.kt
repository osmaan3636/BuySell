package com.astech.buysell.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import com.astech.buysell.R
import com.astech.buysell.models.Product
import com.astech.buysell.viewmodel.CartViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProductSelectionBottomSheet(private val product: Product) : BottomSheetDialogFragment() {

    private val cartViewModel: CartViewModel by activityViewModels()
    private var quantity = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_bottom_sheet_product_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Views
        val tvProductName = view.findViewById<TextView>(R.id.tvProductName)
        val tvProductPrice = view.findViewById<TextView>(R.id.tvProductPrice)
        val tvQuantity = view.findViewById<TextView>(R.id.tvQuantity)
        val btnIncrease = view.findViewById<ImageButton>(R.id.btnIncrease)
        val btnDecrease = view.findViewById<ImageButton>(R.id.btnDecrease)
        val btnAddToCart = view.findViewById<Button>(R.id.btnAddToCart)

        // Set Data
        tvProductName.text = product.name
        tvProductPrice.text = "Selling Price: ${product.formattedSellPrice()}"
        
        // Initial quantity
        cartViewModel.cartItems.value?.find { it.product.id == product.id }?.let {
            quantity = it.quantity
        }
        tvQuantity.text = quantity.toString()

        // Listeners
        btnIncrease.setOnClickListener {
            if (quantity < product.stock) {
                quantity++
                tvQuantity.text = quantity.toString()
            }
        }

        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }

        btnAddToCart.setOnClickListener {
            cartViewModel.addToCart(product, quantity)
            dismiss()
        }
    }
}
