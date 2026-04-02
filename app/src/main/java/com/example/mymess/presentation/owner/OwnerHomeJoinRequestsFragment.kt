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
import com.example.mymess.databinding.FragmentOwnerHomeJoinRequestsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerHomeJoinRequestsFragment : Fragment() {

    private var _binding: FragmentOwnerHomeJoinRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var adapter: OwnerJoinRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerHomeJoinRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Build adapter after attachment to avoid requireActivity() from delegated VM lookup.
        adapter = OwnerJoinRequestsAdapter(
            onApprove = { viewModel.approveJoinRequest(it.request.requestId) },
            onReject = { viewModel.rejectJoinRequest(it.request.requestId) },
        )

        binding.rvJoinRequests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJoinRequests.adapter = adapter
        observeUi()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.joinRequestsState.collect { state ->
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
                                binding.tvInfo.text = "Pending join requests: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.joinUpdateState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Join request updated", Toast.LENGTH_SHORT).show()
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
