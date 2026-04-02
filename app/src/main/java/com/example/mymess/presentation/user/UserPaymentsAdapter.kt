package com.example.mymess.presentation.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.category
import com.example.mymess.databinding.ItemPaymentBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserPaymentsAdapter(
    private val onPayNow: (PaymentRecord) -> Unit,
) : ListAdapter<PaymentRecord, UserPaymentsAdapter.PaymentViewHolder>(PaymentDiff) {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PaymentRecord) {
            val typeLabel = if (item.category() == "cloud_advance") "Cloud Advance" else "Mess Monthly Bill"
            binding.tvTop.text = "$typeLabel | Rs ${item.amount}"
            val due = dateFormat.format(Date(item.dueDate))
            val mealLabel = item.mealName?.takeIf { it.isNotBlank() }?.let { " | Meal: $it" }.orEmpty()
            val method = item.paymentMethod?.let { " | Method: $it" }.orEmpty()
            binding.tvMeta.text = "Due date: $due | Status: ${item.status}$mealLabel$method"

            if (item.category() == "cloud_advance") {
                binding.btnAction.text = if (item.status == "paid") "Paid" else "Awaiting Owner Payment"
                binding.btnAction.isEnabled = false
                binding.btnAction.setOnClickListener(null)
                return
            }

            when (item.status) {
                "paid" -> {
                    binding.btnAction.text = "Paid"
                    binding.btnAction.isEnabled = false
                    binding.btnAction.setOnClickListener(null)
                }
                "payment_submitted" -> {
                    binding.btnAction.text = "Awaiting Confirmation"
                    binding.btnAction.isEnabled = false
                    binding.btnAction.setOnClickListener(null)
                }
                else -> {
                    binding.btnAction.text = "Pay Now"
                    binding.btnAction.isEnabled = true
                    binding.btnAction.setOnClickListener { onPayNow(item) }
                }
            }
        }
    }

    private object PaymentDiff : DiffUtil.ItemCallback<PaymentRecord>() {
        override fun areItemsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem.paymentId == newItem.paymentId

        override fun areContentsTheSame(oldItem: PaymentRecord, newItem: PaymentRecord): Boolean = oldItem == newItem
    }
}

