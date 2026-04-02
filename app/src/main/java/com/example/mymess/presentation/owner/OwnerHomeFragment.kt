package com.example.mymess.presentation.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentOwnerHomeBinding
import com.example.mymess.presentation.user.BannerPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerHomeFragment : Fragment() {

    private var _binding: FragmentOwnerHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerViewModel by viewModels()
    private val bannerAdapter = BannerPagerAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vpOwnerBanners.adapter = bannerAdapter
        binding.vpRequests.adapter = OwnerHomeRequestsPagerAdapter(this)
        setupRequestTabs()
        setupBottomNav()

        observeUi()
        viewModel.loadHome()
    }

    private fun setupRequestTabs() {
        TabLayoutMediator(binding.tabRequests, binding.vpRequests) { tab, position ->
            tab.text = if (position == 0) "Order Requests" else "Join Requests"
        }.attach()
    }

    private fun setupBottomNav() {
        binding.bottomNavOwner.selectedItemId = R.id.nav_owner_home
        binding.bottomNavOwner.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_owner_home -> true
                R.id.nav_owner_orders -> {
                    findNavController().navigate(R.id.ownerPendingOrdersFragment)
                    true
                }
                R.id.nav_owner_meals -> {
                    findNavController().navigate(R.id.ownerMealsFragment)
                    true
                }
                R.id.nav_owner_profile -> {
                    findNavController().navigate(R.id.ownerProfileFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.summaryState.collect { state ->
                        if (state is Resource.Success) {
                            binding.tvEnrolledCount.text = state.data.enrolledUsers.toString()
                            binding.tvPendingCount.text = state.data.pendingOrders.toString()
                            binding.tvEarnings.text = "Rs ${state.data.todaysEarnings}"
                        }
                    }
                }

                launch {
                    viewModel.bannerState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                bannerAdapter.submitList(state.data)
                                binding.tvBannerHint.text = if (state.data.isEmpty()) "No owner banners" else ""
                            }
                            is Resource.Error -> binding.tvBannerHint.text = state.message
                            Resource.Loading -> Unit
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
