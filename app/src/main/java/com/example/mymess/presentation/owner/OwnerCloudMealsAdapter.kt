package com.example.mymess.presentation.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mymess.data.models.Meal
import com.example.mymess.databinding.ItemOwnerCloudMealBinding

class OwnerCloudMealsAdapter(
    private val onEdit: (Meal) -> Unit,
    private val onToggleAvailability: (Meal) -> Unit,
    private val onDelete: (Meal) -> Unit,
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
            val availability = if (item.isAvailable) "Available" else "Unavailable"
            binding.tvMeta.text = "Rs ${item.price} | $availability | ${item.description}"
            binding.btnEdit.setOnClickListener { onEdit(item) }
            binding.btnToggle.text = if (item.isAvailable) "Disable" else "Enable"
            binding.btnToggle.setOnClickListener { onToggleAvailability(item) }
            binding.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    private object MealDiff : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem.mealId == newItem.mealId

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem == newItem
    }
}

