package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.User
import com.example.mymess.databinding.ItemOwnerUserBinding

class OwnerUsersAdapter(
    private val onClick: (User) -> Unit,
) : ListAdapter<User, OwnerUsersAdapter.UserViewHolder>(UserDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemOwnerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemOwnerUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: User) {
            binding.tvName.text = item.name
            binding.tvEmail.text = item.email
            // Using a default avatar placeholder for now
            binding.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_gallery)
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    private object UserDiff : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
