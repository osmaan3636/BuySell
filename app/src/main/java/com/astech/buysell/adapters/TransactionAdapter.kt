package com.astech.buysell.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.astech.buysell.databinding.ItemTransactionBinding
import com.astech.buysell.models.SellTransaction
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class TransactionAdapter : ListAdapter<SellTransaction, TransactionAdapter.TransactionViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SellTransaction) {
            binding.apply {
                tvProductName.text = item.productName
                tvPrice.text = "+${item.finalPrice.toInt()}"
                
                // Show completed status and quantity
                tvDetails.text = "Qty: ${item.quantity} • Completed"
                
                // Format Date
                try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                    val date = inputFormat.parse(item.createdAt)
                    val outputFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                    tvDate.text = if (date != null) outputFormat.format(date) else item.createdAt
                } catch (e: Exception) {
                    tvDate.text = item.createdAt
                }
            }
        }
    }

    class TransactionDiffCallback : DiffUtil.ItemCallback<SellTransaction>() {
        override fun areItemsTheSame(oldItem: SellTransaction, newItem: SellTransaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SellTransaction, newItem: SellTransaction): Boolean {
            return oldItem == newItem
        }
    }
}
