package com.example.mymess.presentation.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentOwnerCloudMealsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerCloudMealsFragment : Fragment() {

    private var _binding: FragmentOwnerCloudMealsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerCloudMealsViewModel by viewModels()
    private var selectedMealId: String? = null
    private val adapter = OwnerCloudMealsAdapter(
        onEdit = { meal ->
            selectedMealId = meal.mealId
            binding.etMealName.setText(meal.name)
            binding.etDescription.setText(meal.description)
            binding.etPrice.setText(meal.price.toString())
            binding.etImage.setText(meal.imageUrl.orEmpty())
            binding.btnAdd.text = "Update Cloud Meal"
            showForm(true)
        },
        onToggleAvailability = { viewModel.toggleAvailability(it) },
        onDelete = { viewModel.deleteMeal(it.mealId) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerCloudMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeals.adapter = adapter
        binding.btnShowForm.setOnClickListener {
            selectedMealId = null
            clearForm()
            binding.btnAdd.text = "Save Cloud Meal"
            showForm(true)
        }
        binding.btnCancel.setOnClickListener {
            selectedMealId = null
            clearForm()
            binding.btnAdd.text = "Save Cloud Meal"
            showForm(false)
        }
        binding.btnAdd.setOnClickListener {
            val price = binding.etPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
            viewModel.saveMeal(
                mealId = selectedMealId,
                name = binding.etMealName.text?.toString().orEmpty(),
                description = binding.etDescription.text?.toString().orEmpty(),
                price = price,
                imageUrl = binding.etImage.text?.toString().orEmpty(),
            )
        }
        observe()
        viewModel.load()
    }

    private fun showForm(show: Boolean) {
        binding.layoutMealForm.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnShowForm.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun clearForm() {
        binding.etMealName.setText("")
        binding.etDescription.setText("")
        binding.etPrice.setText("")
        binding.etImage.setText("")
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mealsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading cloud meals..."
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Cloud meals: ${state.data.size}"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                selectedMealId = null
                                clearForm()
                                binding.btnAdd.text = "Save Cloud Meal"
                                showForm(false)
                                Toast.makeText(requireContext(), "Cloud meal saved", Toast.LENGTH_SHORT).show()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
