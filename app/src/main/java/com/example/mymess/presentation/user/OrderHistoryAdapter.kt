package com.example.mymess.presentation.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.source
import com.example.mymess.data.models.Order
import com.example.mymess.databinding.ItemOrderHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OrderHistoryAdapter(
    private val onOrderClick: ((Order) -> Unit)? = null,
) : ListAdapter<Order, OrderHistoryAdapter.OrderViewHolder>(OrderDiff) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOrderHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Order) {
            binding.tvMeal.text = item.mealName
            val dateText = dateFormat.format(Date(item.createdAt))
            binding.tvMeta.text = "Qty ${item.quantity} | ${item.source().uppercase()} | $dateText"
            binding.tvStatus.text = "Status: ${item.status} | Payment: ${item.paymentStatus}"
            binding.tvPrice.text = "Rs ${item.totalPrice}"
            binding.root.setOnClickListener { onOrderClick?.invoke(item) }
        }
    }

    private object OrderDiff : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem.orderId == newItem.orderId

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem == newItem
    }
}
