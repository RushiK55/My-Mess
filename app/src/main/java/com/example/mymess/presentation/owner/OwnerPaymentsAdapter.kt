package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.category
import com.example.mymess.databinding.ItemPaymentBinding

class OwnerPaymentsAdapter(
    private val onMarkPaid: (PaymentRecord) -> Unit,
) : ListAdapter<PaymentRecord, OwnerPaymentsAdapter.PaymentViewHolder>(PaymentDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PaymentRecord) {
            val displayName = item.userName?.takeIf { it.isNotBlank() } ?: "User ${item.userId.take(6)}..."
            val typeLabel = if (item.category() == "cloud_advance") "Cloud Advance" else "Mess Bill"
            val mealLabel = item.mealName?.takeIf { it.isNotBlank() }?.let { " | Meal: $it" }.orEmpty()
            binding.tvTop.text = "$displayName | $typeLabel | Rs ${item.amount}"
            binding.tvMeta.text = "Status: ${item.status}$mealLabel"
            binding.btnAction.text = if (item.status == "paid") "Paid" else "Mark Paid"
            binding.btnAction.isEnabled = item.status != "paid"
            binding.btnAction.setOnClickListener { onMarkPaid(item) }
        }
    }

    private object PaymentDiff : DiffUtil.ItemCallback<PaymentRecord>() {
        override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem.paymentId == newItem.paymentId

        override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem == newItem
    }
}
