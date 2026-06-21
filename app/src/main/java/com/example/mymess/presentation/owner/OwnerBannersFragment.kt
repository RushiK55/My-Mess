package com.example.mymess.presentation.owner

import android.net.Uri
import android.os.Bundle
import android.transition.TransitionManager
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
import coil.load
import com.example.mymess.MainActivity
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentOwnerBannersBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerBannersFragment : Fragment() {

    private var _binding: FragmentOwnerBannersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerBannersViewModel by viewModels()
    private val adapter = OwnerBannersAdapter { viewModel.delete(it.bannerId) }
    
    private var selectedImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPreview.visibility = View.VISIBLE
            binding.llPlaceholder.visibility = View.GONE
            binding.ivPreview.load(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerBannersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observe()
        viewModel.load()
    }

    private fun setupUI() {
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBanners.adapter = adapter
        
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
            
            val title = binding.etTitle.text?.toString().orEmpty().trim()
            viewModel.save(title, selectedImageUri!!)
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

    private fun validateForm(): Boolean {
        binding.tilTitle.error = null
        val title = binding.etTitle.text?.toString().orEmpty().trim()
        
        if (title.isBlank()) {
            binding.tilTitle.error = "Title is required"
            return false
        }
        
        if (selectedImageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }

    private fun observe() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.bannersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvEmpty.visibility = View.VISIBLE
                                binding.tvEmpty.text = state.message
                            }
                            Resource.Loading -> {
                                mainActivity?.showLoader("Loading banners...")
                            }
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                val banners = state.data
                                adapter.submitList(banners)
                                binding.tvEmpty.visibility = if (banners.isEmpty()) View.VISIBLE else View.GONE
                                binding.tvInfo.text = "You have ${banners.size} active banners"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Uploading & saving banner...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                showForm(false)
                                resetForm()
                                Toast.makeText(requireContext(), "Banner added successfully", Toast.LENGTH_SHORT).show()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun resetForm() {
        binding.etTitle.setText("")
        binding.tilTitle.error = null
        binding.ivPreview.visibility = View.GONE
        binding.llPlaceholder.visibility = View.VISIBLE
        selectedImageUri = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
