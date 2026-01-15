package com.astech.buysell.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.astech.buysell.R
import com.astech.buysell.databinding.ItemStockProductBinding
import com.astech.buysell.models.Product

/**
 * RecyclerView adapter for displaying products in the Stock page
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit = {}
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemStockProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding, onProductClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ProductViewHolder(
        private val binding: ItemStockProductBinding,
        private val onProductClick: (Product) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.apply {
                // Set product name
                tvProductName.text = product.name
                
                // Set prices without repeating logic
                tvBuyPrice.text = product.formattedBuyPrice()
                tvSellPrice.text = product.formattedSellPrice()
                
                // Set stock text
                tvStock.text = "Stock: ${product.stock}"
                
                // Low Stock Logic (Threshold: 5)
                val isLowStock = product.stock <= 5
                
                // 1. Show/Hide "Low" Badge
                tvLowStockBadge.visibility = if (isLowStock) View.VISIBLE else View.GONE
                
                // 2. Dynamic Stock Indicator Styling
                val context = root.context
                
                val stockBgColor = if (isLowStock) {
                    // Light Red background for low stock (#FFEBEE is generic light red)
                    Color.parseColor("#FFEBEE") 
                } else {
                    // Light Blue background for normal stock (#E3F2FD is generic light blue)
                    Color.parseColor("#E3F2FD")
                }
                
                val stockTextColor = if (isLowStock) {
                    // Dark Red text
                    Color.parseColor("#D32F2F")
                } else {
                    // Dark Blue text
                    Color.parseColor("#1976D2")
                }

                // Apply colors
                tvStock.backgroundTintList = ColorStateList.valueOf(stockBgColor)
                tvStock.setTextColor(stockTextColor)
                
                // Load product image
                if (!product.imageUrl.isNullOrEmpty()) {
                    ivProductImage.load(product.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_shopping_bag)
                        error(R.drawable.ic_shopping_bag)
                    }
                } else {
                    ivProductImage.setImageResource(R.drawable.ic_shopping_bag)
                }
                
                // Set click listener
                root.setOnClickListener {
                    onProductClick(product)
                }
            }
        }
    }

    class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
