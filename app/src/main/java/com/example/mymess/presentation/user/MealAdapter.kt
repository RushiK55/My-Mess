package com.example.mymess.presentation.user

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.mymess.data.models.Meal
import com.example.mymess.databinding.ItemMealBinding

class MealAdapter(
    private val onMealClick: (Meal) -> Unit,
    private val subtitleProvider: ((Meal) -> String)? = null,
) : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val binding = ItemMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MealViewHolder(private val binding: ItemMealBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Meal) {
            binding.tvMealName.text = item.name
            val subtitle = subtitleProvider?.invoke(item)?.takeIf { it.isNotBlank() }
            binding.tvMealDesc.text = if (subtitle == null) item.description else "${item.description}\n$subtitle"
            binding.tvMealPrice.text = "Rs ${item.price}"
            binding.ivMeal.load(item.imageUrl)
            binding.root.setOnClickListener { onMealClick(item) }
        }
    }

    private object MealDiff : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem.mealId == newItem.mealId

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean = oldItem == newItem
    }
}
