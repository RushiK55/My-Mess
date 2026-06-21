package com.example.mymess.presentation.admin

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.BottomSheetAdminBannerActionsBinding
import com.example.mymess.databinding.FragmentAdminBannersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminBannersFragment : Fragment() {

    private var _binding: FragmentAdminBannersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminBannersViewModel by viewModels()
    private var editingBanner: Banner? = null
    private var allBanners: List<Banner> = emptyList()
    private var activeFilter: String = "all"
    
    private var selectedImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPlaceholder.visibility = View.GONE
            binding.ivPreview.load(uri)
        }
    }

    private val adapter = AdminBannersAdapter(
        onItemClick = { banner -> showBannerActions(banner) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminBannersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeUi()
        viewModel.load()
    }

    private fun setupUI() {
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBanners.adapter = adapter

        // Setup Target Role Dropdown
        val roles = arrayOf("all", "user", "owner", "admin")
        val adapterRoles = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, roles)
        binding.etRole.setAdapter(adapterRoles)

        binding.fabAddBanner.setOnClickListener {
            showForm(true)
        }

        binding.btnCancel.setOnClickListener {
            showForm(false)
            resetForm()
        }
        
        binding.cardImagePicker.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnSave.setOnClickListener {
            if (!validateForm()) return@setOnClickListener
            
            viewModel.save(
                editingBannerId = editingBanner?.bannerId,
                title = binding.etTitle.text?.toString().orEmpty(),
                imageUri = selectedImageUri,
                existingImageUrl = editingBanner?.imageUrl,
                targetRole = binding.etRole.text?.toString().orEmpty(),
                isActive = binding.switchActive.isChecked,
            )
        }

        binding.chipFilterGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            activeFilter = when (checkedIds.firstOrNull()) {
                binding.chipUser.id -> "user"
                binding.chipOwner.id -> "owner"
                binding.chipAdmin.id -> "admin"
                else -> "all"
            }
            applyFilter()
        }
    }

    private fun showForm(show: Boolean) {
        TransitionManager.beginDelayedTransition(binding.contentContainer)
        binding.cardForm.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.fabAddBanner.hide()
            binding.etTitle.requestFocus()
        } else {
            binding.fabAddBanner.show()
        }
    }

    private fun applyFilter() {
        val filtered = if (activeFilter == "all") {
            allBanners
        } else {
            allBanners.filter { it.targetRole.equals(activeFilter, ignoreCase = true) }
        }
        adapter.submitList(filtered)
        binding.tvInfo.text = "Showing ${filtered.size} banners"
    }

    private fun showBannerActions(banner: Banner) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAdminBannerActionsBinding.inflate(layoutInflater)
        
        sheetBinding.tvSheetTitle.text = banner.title
        sheetBinding.tvSheetTarget.text = "Target: ${banner.targetRole.replaceFirstChar { it.uppercase() }}"
        sheetBinding.tvSheetStatus.text = if (banner.isActive) "Active" else "Inactive"
        sheetBinding.tvSheetStatus.setTextColor(
            requireContext().getColor(if (banner.isActive) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
        
        sheetBinding.ivBannerDetail.load(banner.imageUrl) {
            placeholder(R.color.admin_divider)
            error(R.color.admin_divider)
        }

        sheetBinding.btnEditBanner.setOnClickListener {
            dialog.dismiss()
            startEditing(banner)
        }

        sheetBinding.btnDeleteBanner.setOnClickListener {
            dialog.dismiss()
            confirmDelete(banner)
        }

        sheetBinding.btnCloseSheet.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bannersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                            Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                allBanners = state.data
                                applyFilter()
                            }
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                showFormMessage(state.message, isError = true)
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.btnSave.isEnabled = false
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.btnSave.isEnabled = true
                                showFormMessage(state.data, isError = false)
                                showForm(false)
                                resetForm()
                                viewModel.load()
                            }
                            null -> {
                                binding.btnSave.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startEditing(banner: Banner) {
        editingBanner = banner
        binding.tvFormTitle.text = "Edit Banner"
        binding.etTitle.setText(banner.title)
        
        // Show existing image
        binding.ivPreview.visibility = View.VISIBLE
        binding.llPlaceholder.visibility = View.GONE
        binding.ivPreview.load(banner.imageUrl)
        selectedImageUri = null // Reset selected URI, use existing URL unless changed
        
        binding.etRole.setText(banner.targetRole, false)
        binding.switchActive.isChecked = banner.isActive
        binding.btnSave.text = "Update Banner"
        showForm(true)
    }

    private fun confirmDelete(banner: Banner) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Banner")
            .setMessage("Are you sure you want to delete '${banner.title}'?")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(banner.bannerId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetForm() {
        editingBanner = null
        selectedImageUri = null
        binding.tvFormTitle.text = "Create New Banner"
        binding.etTitle.setText("")
        binding.ivPreview.visibility = View.GONE
        binding.llPlaceholder.visibility = View.VISIBLE
        binding.etRole.setText("all", false)
        binding.switchActive.isChecked = true
        binding.btnSave.text = "Save Banner"
        clearValidationErrors()
    }

    private fun validateForm(): Boolean {
        clearValidationErrors()
        val title = binding.etTitle.text?.toString().orEmpty().trim()
        var isValid = true

        if (title.length < 3) {
            binding.tilTitle.error = "Title too short"
            isValid = false
        }
        
        if (selectedImageUri == null && (editingBanner == null || editingBanner?.imageUrl.isNullOrBlank())) {
            showFormMessage("Please select an image", isError = true)
            isValid = false
        }

        return isValid
    }

    private fun clearValidationErrors() {
        binding.tilTitle.error = null
        binding.tvFormMessage.visibility = View.GONE
    }

    private fun showFormMessage(message: String, isError: Boolean) {
        binding.tvFormMessage.visibility = View.VISIBLE
        binding.tvFormMessage.text = message
        binding.tvFormMessage.setTextColor(
            requireContext().getColor(if (isError) android.R.color.holo_red_dark else android.R.color.holo_green_dark)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
