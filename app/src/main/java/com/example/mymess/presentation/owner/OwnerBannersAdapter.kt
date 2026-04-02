package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.ItemOwnerBannerManageBinding

class OwnerBannersAdapter(
    private val onDelete: (Banner) -> Unit,
) : ListAdapter<Banner, OwnerBannersAdapter.BannerViewHolder>(BannerDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemOwnerBannerManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BannerViewHolder(private val binding: ItemOwnerBannerManageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Banner) {
            binding.tvTitle.text = item.title
            binding.tvMeta.text = "target=${item.targetRole} | active=${item.isActive}"
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object BannerDiff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem.bannerId == newItem.bannerId

        override fun areContentsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem == newItem
    }
}

