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
import com.example.mymess.databinding.FragmentOwnerRequestsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerRequestsFragment : Fragment() {

    private var _binding: FragmentOwnerRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerRequestsViewModel by viewModels()
    private val adapter = OwnerJoinRequestsAdapter(
        onApprove = { viewModel.approve(it.request.requestId) },
        onReject = { viewModel.reject(it.request.requestId) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRequests.adapter = adapter
        binding.btnRefresh.setOnClickListener { viewModel.loadRequests() }
        observeState()
        viewModel.loadRequests()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.requestsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading join requests..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Pending requests: ${state.data.size}"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Request updated", Toast.LENGTH_SHORT).show()
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

