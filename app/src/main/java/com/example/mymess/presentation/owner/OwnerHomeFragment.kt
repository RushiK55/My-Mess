package com.example.mymess.presentation.owner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.mymess.MainActivity
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

    private val sliderHandler = Handler(Looper.getMainLooper())
    private val sliderRunnable = Runnable {
        if (_binding != null && bannerAdapter.itemCount > 0) {
            val nextItem = (binding.vpOwnerBanners.currentItem + 1) % bannerAdapter.itemCount
            binding.vpOwnerBanners.currentItem = nextItem
        }
    }

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
        TabLayoutMediator(binding.tabBannerIndicator, binding.vpOwnerBanners) { _, _ -> }.attach()

        binding.vpOwnerBanners.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                sliderHandler.removeCallbacks(sliderRunnable)
                sliderHandler.postDelayed(sliderRunnable, 3000)
            }
        })
        binding.btnAnalytics.setOnClickListener {
            findNavController().navigate(R.id.action_ownerHomeFragment_to_ownerAnalyticsFragment)
        }
        binding.btnPendingOrders.setOnClickListener {
            findNavController().navigate(R.id.action_ownerHomeFragment_to_ownerPendingOrdersFragment)
        }
        binding.btnEnrolledUser.setOnClickListener {
            findNavController().navigate(R.id.action_ownerHomeFragment_to_ownerEnrolledUsersFragment)
        }

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
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.summaryState.collect { state ->
                        when (state) {
                            is Resource.Loading -> mainActivity?.showLoader("Loading dashboard...")
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                            }

                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                binding.tvEnrolledCount.text = state.data.enrolledUsers.toString()
                                binding.tvPendingCount.text = state.data.pendingOrders.toString()
                                binding.tvEarnings.text = "Rs ${state.data.todaysEarnings}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.bannerState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                bannerAdapter.submitList(state.data)
                                binding.tvBannerHint.text =
                                    if (state.data.isEmpty()) "No owner banners" else ""
                                binding.tabBannerIndicator.visibility =
                                    if (state.data.size > 1) View.VISIBLE else View.GONE
                            }

                            is Resource.Error -> binding.tvBannerHint.text = state.message
                            Resource.Loading -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        sliderHandler.removeCallbacks(sliderRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (bannerAdapter.itemCount > 0) {
            sliderHandler.postDelayed(sliderRunnable, 3000)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
