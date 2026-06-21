package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mymess.R
import com.example.mymess.data.models.Meal
import com.example.mymess.databinding.ItemOwnerCloudMealBinding

class OwnerCloudMealsAdapter(
    private val onItemClick: (Meal) -> Unit,
) : ListAdapter<Meal, OwnerCloudMealsAdapter.MealViewHolder>(MealDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemOwnerCloudMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealViewHolder(private val binding: ItemOwnerCloudMealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Meal) {
            binding.tvName.text = item.name
            binding.tvDescription.text = item.description
            binding.tvPrice.text = "Rs ${item.price}"
            
            val statusText = if (item.isAvailable) "Available" else "Unavailable"
            binding.tvStatus.text = statusText
            binding.tvStatus.setTextColor(
                if (item.isAvailable) 
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                else 
                    binding.root.context.getColor(android.R.color.holo_red_dark)
            )

            binding.ivMeal.load(item.imageUrl) {
                placeholder(android.R.color.darker_gray)
                error(android.R.color.darker_gray)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    private object MealDiff : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem.mealId == newItem.mealId

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem == newItem
    }
}
