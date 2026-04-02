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
import com.example.mymess.databinding.FragmentOwnerBannersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerBannersFragment : Fragment() {

    private var _binding: FragmentOwnerBannersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerBannersViewModel by viewModels()
    private val adapter = OwnerBannersAdapter { viewModel.delete(it.bannerId) }

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
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBanners.adapter = adapter
        binding.btnSave.setOnClickListener {
            viewModel.save(
                binding.etTitle.text?.toString().orEmpty(),
                binding.etImage.text?.toString().orEmpty(),
            )
        }
        observe()
        viewModel.load()
    }

    private fun observe() {
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
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                binding.etTitle.setText("")
                                binding.etImage.setText("")
                                Toast.makeText(requireContext(), "Banner updated", Toast.LENGTH_SHORT).show()
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

