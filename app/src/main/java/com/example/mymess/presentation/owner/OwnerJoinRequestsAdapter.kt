package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.databinding.ItemJoinRequestBinding

class OwnerJoinRequestsAdapter(
    private val onApprove: (JoinRequestWithUser) -> Unit,
    private val onReject: (JoinRequestWithUser) -> Unit,
) : ListAdapter<JoinRequestWithUser, OwnerJoinRequestsAdapter.RequestViewHolder>(RequestDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemJoinRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RequestViewHolder(private val binding: ItemJoinRequestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: JoinRequestWithUser) {
            binding.tvTop.text = item.user?.name ?: "User ${item.request.userId.take(6)}..."
            binding.tvMeta.text = item.user?.email ?: "No email"
            binding.btnApprove.setOnClickListener { onApprove(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
        }
    }

    private object RequestDiff : DiffUtil.ItemCallback<JoinRequestWithUser>() {
        override fun areItemsTheSame(oldItem: JoinRequestWithUser, newItem: JoinRequestWithUser): Boolean {
            return oldItem.request.requestId == newItem.request.requestId
        }

        override fun areContentsTheSame(oldItem: JoinRequestWithUser, newItem: JoinRequestWithUser): Boolean {
            return oldItem == newItem
        }
    }
}

