package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Order
import com.example.mymess.databinding.ItemOwnerOrderBinding

class OwnerPendingOrdersAdapter(
    private val onAdvance: (Order) -> Unit,
) : ListAdapter<Order, OwnerPendingOrdersAdapter.OrderViewHolder>(OrderDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOwnerOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class OrderViewHolder(private val binding: ItemOwnerOrderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Order) {
            binding.tvMeal.text = item.mealName
            binding.tvMeta.text = "Qty ${item.quantity} | Rs ${item.totalPrice}"
            binding.tvStatus.text = "Current: ${item.status}"
            binding.btnAdvance.text = when (item.status) {
                "pending" -> "Accept"
                "accepted" -> "Start Preparing"
                "preparing" -> "Mark Ready"
                "ready" -> "Deliver"
                else -> "Done"
            }
            binding.btnAdvance.isEnabled = item.status != "delivered"
            binding.btnAdvance.setOnClickListener { onAdvance(item) }
        }
    }

    private object OrderDiff : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem.orderId == newItem.orderId

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem == newItem
    }
}

