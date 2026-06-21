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
import com.example.mymess.MainActivity
import com.example.mymess.core.Resource
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.databinding.BottomSheetJoinRequestDetailsBinding
import com.example.mymess.databinding.FragmentOwnerRequestsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerRequestsFragment : Fragment() {

    private var _binding: FragmentOwnerRequestsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerRequestsViewModel by viewModels()
    private val adapter = OwnerJoinRequestsAdapter { request -> showJoinRequestDetails(request) }

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

    private fun showJoinRequestDetails(item: JoinRequestWithUser) {
        val sheetBinding = BottomSheetJoinRequestDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetUserName.text = item.user?.name ?: "User ${item.request.userId.takeLast(6)}"
        sheetBinding.tvSheetUserEmail.text = item.user?.email ?: "No email available"
        sheetBinding.tvSheetUserPhone.text = item.user?.phone ?: "No phone available"

        sheetBinding.btnSheetApprove.setOnClickListener {
            viewModel.approve(item.request.requestId)
            dialog.dismiss()
        }

        sheetBinding.btnSheetReject.setOnClickListener {
            viewModel.reject(item.request.requestId)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeState() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.requestsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                mainActivity?.showLoader("Loading join requests...")
                                binding.tvInfo.text = "Loading join requests..."
                            }

                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Pending requests: ${state.data.size}"
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
                            Resource.Loading -> mainActivity?.showLoader("Updating request...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "Request updated", Toast.LENGTH_SHORT).show()
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
