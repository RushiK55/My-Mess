package com.example.mymess.presentation.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.ItemAdminBannerManageBinding

class AdminBannersAdapter(
    private val onEdit: (Banner) -> Unit,
    private val onDelete: (Banner) -> Unit,
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
            binding.tvMeta.text = "target=${item.targetRole} | active=${item.isActive}"
            binding.btnEdit.setOnClickListener { onEdit(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object BannerDiff : DiffUtil.ItemCallback<Banner>() {
        override fun areItemsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem.bannerId == newItem.bannerId

        override fun areContentsTheSame(oldItem: Banner, newItem: Banner): Boolean = oldItem == newItem
    }
}

