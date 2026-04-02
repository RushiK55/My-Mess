package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Order
import com.example.mymess.databinding.ItemOwnerRequestBinding

class OwnerOrderRequestsAdapter(
    private val onAccept: (Order) -> Unit,
    private val onReject: (Order) -> Unit,
) : ListAdapter<Order, OwnerOrderRequestsAdapter.RequestViewHolder>(OrderDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemOwnerRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(private val binding: ItemOwnerRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Order) {
            binding.tvMeal.text = item.mealName
            binding.tvMeta.text = "Qty ${item.quantity} | Rs ${item.totalPrice}"
            binding.btnAccept.setOnClickListener { onAccept(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
        }
    }

    private object OrderDiff : DiffUtil.ItemCallback<Order>() {
        override fun areItemsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem.orderId == newItem.orderId

        override fun areContentsTheSame(oldItem: Order, newItem: Order): Boolean = oldItem == newItem
    }
}

