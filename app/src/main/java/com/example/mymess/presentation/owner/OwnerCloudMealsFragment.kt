package com.example.mymess.presentation.owner

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import coil.load
import com.example.mymess.MainActivity
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Meal
import com.example.mymess.databinding.BottomSheetOwnerMealDetailsBinding
import com.example.mymess.databinding.FragmentOwnerCloudMealsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerCloudMealsFragment : Fragment() {

    private var _binding: FragmentOwnerCloudMealsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerCloudMealsViewModel by viewModels()
    
    private var selectedMealId: String? = null
    private var existingImageUrl: String? = null
    private var selectedImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPlaceholder.visibility = View.GONE
            binding.ivPreview.load(uri)
        }
    }

    private val adapter = OwnerCloudMealsAdapter { showMealDetails(it) }

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
        setupUI()
        observe()
        viewModel.load()
    }

    private fun setupUI() {
        binding.rvMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeals.adapter = adapter
        
        binding.cardImagePicker.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnShowForm.setOnClickListener {
            selectedMealId = null
            existingImageUrl = null
            clearForm()
            binding.tvFormTitle.text = "Add Cloud Meal"
            binding.btnAdd.text = "Save Meal"
            showForm(true)
        }
        
        binding.btnCancel.setOnClickListener {
            selectedMealId = null
            existingImageUrl = null
            clearForm()
            showForm(false)
        }
        
        binding.btnAdd.setOnClickListener {
            val price = binding.etPrice.text?.toString()?.toDoubleOrNull() ?: 0.0
            val name = binding.etMealName.text?.toString().orEmpty().trim()
            
            if (name.isEmpty() || price <= 0.0) {
                Toast.makeText(requireContext(), "Enter valid name and price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (selectedImageUri == null && existingImageUrl == null) {
                Toast.makeText(requireContext(), "Please select a meal photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveMeal(
                mealId = selectedMealId,
                name = name,
                description = binding.etDescription.text?.toString().orEmpty().trim(),
                price = price,
                imageUri = selectedImageUri,
                existingImageUrl = existingImageUrl
            )
        }
    }

    private fun showMealDetails(meal: Meal) {
        val sheetBinding = BottomSheetOwnerMealDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.ivMeal.load(meal.imageUrl) {
            placeholder(android.R.color.darker_gray)
            error(android.R.color.darker_gray)
        }
        sheetBinding.tvMealName.text = meal.name
        sheetBinding.tvMealPrice.text = "Rs ${meal.price}"
        sheetBinding.tvMealDescription.text = meal.description
        
        val statusText = if (meal.isAvailable) "AVAILABLE" else "UNAVAILABLE"
        sheetBinding.tvMealStatus.text = statusText
        sheetBinding.tvMealStatus.setTextColor(
            if (meal.isAvailable) 
                requireContext().getColor(android.R.color.holo_green_dark)
            else 
                requireContext().getColor(android.R.color.holo_red_dark)
        )
        sheetBinding.btnToggle.text = if (meal.isAvailable) "Disable" else "Enable"

        sheetBinding.btnEdit.setOnClickListener {
            selectedMealId = meal.mealId
            existingImageUrl = meal.imageUrl
            selectedImageUri = null
            
            binding.tvFormTitle.text = "Edit Cloud Meal"
            binding.etMealName.setText(meal.name)
            binding.etDescription.setText(meal.description)
            binding.etPrice.setText(meal.price.toString())
            
            if (!meal.imageUrl.isNullOrBlank()) {
                binding.ivPreview.visibility = View.VISIBLE
                binding.llPlaceholder.visibility = View.GONE
                binding.ivPreview.load(meal.imageUrl)
            } else {
                binding.ivPreview.visibility = View.GONE
                binding.llPlaceholder.visibility = View.VISIBLE
            }
            
            binding.btnAdd.text = "Update Meal"
            showForm(true)
            dialog.dismiss()
        }

        sheetBinding.btnToggle.setOnClickListener {
            viewModel.toggleAvailability(meal)
            dialog.dismiss()
        }

        sheetBinding.btnDelete.setOnClickListener {
            viewModel.deleteMeal(meal.mealId)
            dialog.dismiss()
        }

        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun showForm(show: Boolean) {
        TransitionManager.beginDelayedTransition(binding.contentContainer, AutoTransition())
        binding.layoutMealForm.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.btnShowForm.hide()
            binding.contentContainer.parent.requestLayout()
        } else {
            binding.btnShowForm.show()
        }
    }

    private fun clearForm() {
        binding.etMealName.setText("")
        binding.etDescription.setText("")
        binding.etPrice.setText("")
        binding.ivPreview.visibility = View.GONE
        binding.llPlaceholder.visibility = View.VISIBLE
        selectedImageUri = null
    }

    private fun observe() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.mealsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                mainActivity?.showLoader("Loading cloud meals...")
                            }
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "You have ${state.data.size} items in cloud menu"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Uploading & saving...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                selectedMealId = null
                                existingImageUrl = null
                                clearForm()
                                showForm(false)
                                Toast.makeText(requireContext(), "Cloud menu updated", Toast.LENGTH_SHORT).show()
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
