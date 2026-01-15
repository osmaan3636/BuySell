package com.astech.buysell.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.astech.buysell.R
import com.astech.buysell.databinding.ItemCartProductBinding
import com.astech.buysell.models.CartItem

class CartAdapter(
    private val onRemoveClick: (CartItem) -> Unit
) : ListAdapter<CartItem, CartAdapter.CartViewHolder>(CartDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CartViewHolder(private val binding: ItemCartProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CartItem) {
            binding.apply {
                tvProductName.text = item.product.name
                tvQuantityInfo.text = "${item.quantity} x ${item.product.formattedSellPrice()}"
                
                val total = item.product.sellPrice * item.quantity
                tvTotalPrice.text = "${total.toInt()} TK"
                
                if (!item.product.imageUrl.isNullOrEmpty()) {
                    ivProduct.load(item.product.imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_shopping_bag)
                        error(R.drawable.ic_shopping_bag)
                    }
                } else {
                    ivProduct.setImageResource(R.drawable.ic_shopping_bag)
                }

                btnRemove.setOnClickListener {
                    onRemoveClick(item)
                }
            }
        }
    }

    class CartDiffCallback : DiffUtil.ItemCallback<CartItem>() {
        override fun areItemsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem.product.id == newItem.product.id
        }

        override fun areContentsTheSame(oldItem: CartItem, newItem: CartItem): Boolean {
            return oldItem == newItem
        }
    }
}
