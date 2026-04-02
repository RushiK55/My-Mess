package com.example.mymess.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.User
import com.example.mymess.databinding.ItemAdminUserBinding

class AdminUsersAdapter(
    private val onOpenDetails: (User) -> Unit,
) : ListAdapter<User, AdminUsersAdapter.UserViewHolder>(UserDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAdminUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemAdminUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: User) {
            binding.tvTop.text = "${item.name} (${item.role})"
            binding.tvMeta.text = "${item.email} | ${item.status}"
            binding.tvHint.text = "Tap to view details and actions"
            binding.root.setOnClickListener { onOpenDetails(item) }
        }
    }

    private object UserDiff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}

