package com.example.mymess.presentation.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Banner
import com.example.mymess.databinding.FragmentAdminBannersBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminBannersFragment : Fragment() {

    private var _binding: FragmentAdminBannersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminBannersViewModel by viewModels()
    private var editingBanner: Banner? = null

    private val adapter = AdminBannersAdapter(
        onEdit = { banner -> startEditing(banner) },
        onDelete = { banner -> confirmDelete(banner) },
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
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBanners.adapter = adapter

        binding.btnSave.setOnClickListener {
            if (!validateForm()) {
                return@setOnClickListener
            }
            viewModel.save(
                editingBannerId = editingBanner?.bannerId,
                title = binding.etTitle.text?.toString().orEmpty(),
                imageUrl = binding.etImage.text?.toString().orEmpty(),
                targetRole = binding.etRole.text?.toString().orEmpty(),
                isActive = binding.switchActive.isChecked,
            )
        }

        binding.btnReset.setOnClickListener { resetForm() }
        binding.btnRefresh.setOnClickListener { viewModel.load() }

        observeUi()
        viewModel.load()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bannersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading banners..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Banners: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> showFormMessage(state.message, isError = true)
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                showFormMessage(state.data, isError = false)
                                resetForm()
                            }

                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun startEditing(banner: Banner) {
        editingBanner = banner
        clearValidationErrors()
        binding.etTitle.setText(banner.title)
        binding.etImage.setText(banner.imageUrl)
        binding.etRole.setText(banner.targetRole)
        binding.switchActive.isChecked = banner.isActive
        binding.btnSave.text = "Update Banner"
    }

    private fun confirmDelete(banner: Banner) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete banner")
            .setMessage("Delete '${banner.title}'?")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(banner.bannerId) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetForm() {
        editingBanner = null
        clearValidationErrors()
        binding.etTitle.setText("")
        binding.etImage.setText("")
        binding.etRole.setText("all")
        binding.switchActive.isChecked = true
        binding.btnSave.text = "Create Banner"
    }

    private fun validateForm(): Boolean {
        clearValidationErrors()
        val title = binding.etTitle.text?.toString().orEmpty().trim()
        val image = binding.etImage.text?.toString().orEmpty().trim()
        val role = binding.etRole.text?.toString().orEmpty().trim().lowercase()
        var isValid = true

        if (title.length < 3) {
            binding.tilTitle.error = "Enter at least 3 characters"
            isValid = false
        }

        val isHttpUrl = image.startsWith("http://") || image.startsWith("https://")
        if (!isHttpUrl) {
            binding.tilImage.error = "Image URL must start with http:// or https://"
            isValid = false
        }

        if (role !in setOf("user", "owner", "admin", "all")) {
            binding.tilRole.error = "Use one of: user, owner, admin, all"
            isValid = false
        }

        if (!isValid) {
            showFormMessage("Please fix highlighted fields", isError = true)
        }
        return isValid
    }

    private fun clearValidationErrors() {
        binding.tilTitle.error = null
        binding.tilImage.error = null
        binding.tilRole.error = null
        binding.tvFormMessage.visibility = View.GONE
    }

    private fun showFormMessage(message: String, isError: Boolean) {
        binding.tvFormMessage.visibility = View.VISIBLE
        binding.tvFormMessage.text = message
        binding.tvFormMessage.setTextColor(
            if (isError) requireContext().getColor(android.R.color.holo_red_dark)
            else requireContext().getColor(android.R.color.holo_green_dark),
        )
        Snackbar.make(binding.root, message, if (isError) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


