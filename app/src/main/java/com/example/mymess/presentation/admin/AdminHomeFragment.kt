package com.example.mymess.presentation.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.User
import com.example.mymess.databinding.FragmentAdminHomeBinding
import com.example.mymess.presentation.user.BannerPagerAdapter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminHomeFragment : Fragment() {

    private var _binding: FragmentAdminHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AdminViewModel by viewModels()
    private val bannerAdapter = BannerPagerAdapter()
    private val adapter = PendingOwnerAdapter(
        onApprove = { viewModel.approveOwner(it.uid) },
        onReject = { viewModel.rejectOwner(it.uid) },
        onOpenDetails = { showOwnerDetails(it) },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPendingOwners.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPendingOwners.adapter = adapter
        binding.rvBanners.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvBanners.adapter = bannerAdapter
        binding.btnRefresh.setOnClickListener {
            viewModel.loadPendingOwners()
            viewModel.loadBanners()
            viewModel.loadSummary()
        }
        binding.btnUsers.setOnClickListener {
            findNavController().navigate(R.id.action_adminHomeFragment_to_adminUsersFragment)
        }
        binding.btnBanners.setOnClickListener {
            findNavController().navigate(R.id.action_adminHomeFragment_to_adminBannersFragment)
        }
        binding.btnAnalytics.setOnClickListener {
            findNavController().navigate(R.id.action_adminHomeFragment_to_adminAnalyticsFragment)
        }
        observeUi()
        viewModel.loadBanners()
        viewModel.loadPendingOwners()
        viewModel.loadSummary()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pendingOwnersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvAdminInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvAdminInfo.text = "Loading pending owner approvals..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvAdminInfo.text = "Pending owners: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.bannerState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                bannerAdapter.submitList(state.data)
                                binding.tvBannerInfo.text = if (state.data.isEmpty()) "No admin banners found" else "Admin banners"
                            }

                            is Resource.Error -> binding.tvBannerInfo.text = state.message
                            Resource.Loading -> binding.tvBannerInfo.text = "Loading banners..."
                        }
                    }
                }

                launch {
                    viewModel.summaryState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                val d = state.data
                                binding.tvSummaryUsers.text = "Users: ${d.totalUsers}"
                                binding.tvSummaryOwners.text = "Owners: ${d.totalOwners}"
                                binding.tvSummaryOrders.text = "Orders: ${d.totalOrders}"
                                binding.tvSummaryRevenue.text = "Revenue: Rs ${String.format(Locale.getDefault(), "%.2f", d.totalRevenue)}"
                            }

                            is Resource.Error -> {
                                binding.tvSummaryUsers.text = state.message
                                binding.tvSummaryOwners.text = ""
                                binding.tvSummaryOrders.text = ""
                                binding.tvSummaryRevenue.text = ""
                            }

                            Resource.Loading -> {
                                binding.tvSummaryUsers.text = "Loading summary..."
                                binding.tvSummaryOwners.text = ""
                                binding.tvSummaryOrders.text = ""
                                binding.tvSummaryRevenue.text = ""
                            }
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Owner status updated", Toast.LENGTH_SHORT).show()
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun showOwnerDetails(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle(user.name)
            .setMessage(
                "Email: ${user.email}\n" +
                    "Phone: ${user.phone}\n" +
                    "Status: ${user.status}",
            )
            .setPositiveButton("Approve") { _, _ -> viewModel.approveOwner(user.uid) }
            .setNeutralButton("Reject") { _, _ -> viewModel.rejectOwner(user.uid) }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

