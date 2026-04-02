package com.example.mymess.presentation.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.ItemBannerBinding

class BannerPagerAdapter : RecyclerView.Adapter<BannerPagerAdapter.BannerViewHolder>() {

    private val items = mutableListOf<Banner>()

    fun submitList(data: List<Banner>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class BannerViewHolder(private val binding: ItemBannerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Banner) {
            binding.tvBannerTitle.text = item.title
            binding.ivBanner.load(item.imageUrl)
        }
    }
}

