package com.example.mymess.presentation.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Mess
import com.example.mymess.databinding.ItemMessBinding

class MessBrowseAdapter(
    private val onRequestJoin: (Mess) -> Unit,
    private val onViewDetails: (Mess) -> Unit,
) : ListAdapter<Mess, MessBrowseAdapter.MessViewHolder>(MessDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessViewHolder {
        val binding = ItemMessBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessViewHolder(private val binding: ItemMessBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Mess) {
            binding.tvMessName.text = item.name
            binding.tvMessAddress.text = "${item.address}, ${item.city}"
            binding.tvMessContact.text = item.contact
            binding.tvMessDesc.text = item.description
            binding.btnRequestJoin.setOnClickListener { onRequestJoin(item) }
            binding.btnViewDetails.setOnClickListener { onViewDetails(item) }
            binding.root.setOnClickListener { onViewDetails(item) }
        }
    }

    private object MessDiff : DiffUtil.ItemCallback<Mess>() {
        override fun areItemsTheSame(oldItem: Mess, newItem: Mess): Boolean = oldItem.messId == newItem.messId

        override fun areContentsTheSame(oldItem: Mess, newItem: Mess): Boolean = oldItem == newItem
    }
}
