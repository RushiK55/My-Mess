package com.example.mymess.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mymess.R
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.ItemAdminBannerManageBinding

class AdminBannersAdapter(
    private val onItemClick: (Banner) -> Unit,
) : ListAdapter<Banner, AdminBannersAdapter.BannerViewHolder>(BannerDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemAdminBannerManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BannerViewHolder(private val binding: ItemAdminBannerManageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Banner) {
            binding.tvTitle.text = item.title
            binding.tvMeta.text = "Target: ${item.targetRole.replaceFirstChar { it.uppercase() }} | ${if (item.isActive) "Active" else "Inactive"}"
            
            binding.ivBannerPreview.load(item.imageUrl) {
                crossfade(true)
                placeholder(R.color.admin_divider)
                error(R.color.admin_divider)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private object BannerDiff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem.bannerId == newItem.bannerId
        override fun areContentsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem == newItem
    }
}
