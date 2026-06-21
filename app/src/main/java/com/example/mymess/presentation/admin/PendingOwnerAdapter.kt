package com.example.mymess.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mymess.R
import com.example.mymess.data.models.User
import com.example.mymess.databinding.ItemPendingOwnerBinding

class PendingOwnerAdapter(
    private val onApprove: (User) -> Unit,
    private val onReject: (User) -> Unit,
    private val onOpenDetails: (User) -> Unit,
) : ListAdapter<User, PendingOwnerAdapter.PendingOwnerViewHolder>(OwnerDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOwnerViewHolder {
        val binding = ItemPendingOwnerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingOwnerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOwnerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PendingOwnerViewHolder(private val binding: ItemPendingOwnerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: User) {
            binding.tvName.text = item.name
            binding.tvMeta.text = "${item.email} | ${item.phone}"
            
            binding.ivUserAvatar.load(item.profilePic) {
                placeholder(android.R.drawable.ic_menu_myplaces)
                error(android.R.drawable.ic_menu_myplaces)
                crossfade(true)
            }
            
            binding.btnApprove.setOnClickListener { onApprove(item) }
            binding.btnReject.setOnClickListener { onReject(item) }
            binding.root.setOnClickListener { onOpenDetails(item) }
        }
    }

    private object OwnerDiff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
