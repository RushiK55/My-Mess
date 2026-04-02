package com.example.mymess.presentation.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.category
import com.example.mymess.databinding.BottomSheetMealDetailsBinding
import com.example.mymess.databinding.FragmentUserHomeBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserHomeFragment : Fragment() {

    private var _binding: FragmentUserHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserHomeViewModel by viewModels()
    private val cloudAdapter = MealAdapter(
        onMealClick = { meal -> showMealDetailsBottomSheet(meal) },
        subtitleProvider = { meal ->
            val mess = allMesses.firstOrNull { it.messId == meal.messId }
            if (mess == null) "" else "From ${mess.name}, ${mess.city}"
        },
    )
    private val messMealAdapter = MealAdapter(onMealClick = { meal -> showMealDetailsBottomSheet(meal) })
    private val bannerAdapter = BannerPagerAdapter()

    private var allCloudMeals: List<Meal> = emptyList()
    private var allMesses: List<Mess> = emptyList()
    private var messMeals: List<Meal> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.vpBanners.adapter = bannerAdapter
        binding.rvMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMeals.adapter = cloudAdapter
        binding.rvMeals.isNestedScrollingEnabled = false
        binding.rvMessMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMessMeals.adapter = messMealAdapter
        binding.rvMessMeals.isNestedScrollingEnabled = false

        binding.tabsHome.addTab(binding.tabsHome.newTab().setText("Mess Section"))
        binding.tabsHome.addTab(binding.tabsHome.newTab().setText("Cloud Section"))
        binding.tabsHome.getTabAt(0)?.select()
        showTab(0)
        binding.tabsHome.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showTab(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        binding.btnRefreshCloud.setOnClickListener { viewModel.loadCloudMeals() }
        binding.bottomNavUser.selectedItemId = R.id.nav_user_home
        binding.bottomNavUser.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_user_home -> true
                R.id.nav_user_orders -> {
                    findNavController().navigate(R.id.action_userHomeFragment_to_userOrdersFragment)
                    true
                }
                R.id.nav_user_messes -> {
                    findNavController().navigate(R.id.action_userHomeFragment_to_userMessesFragment)
                    true
                }
                R.id.nav_user_profile -> {
                    findNavController().navigate(R.id.action_userHomeFragment_to_userProfileFragment)
                    true
                }
                else -> false
            }
        }

        binding.etSearchMeals.addTextChangedListener { applyCloudSearch() }

        observeUi()
        viewModel.loadApprovedMesses()
        viewModel.loadUserHome()
    }

    private fun showTab(position: Int) {
        val isMessTab = position == 0
        binding.layoutMessSection.visibility = if (isMessTab) View.VISIBLE else View.GONE
        binding.layoutCloudSection.visibility = if (isMessTab) View.GONE else View.VISIBLE
        binding.homeScroll.post { binding.homeScroll.fullScroll(NestedScrollView.FOCUS_UP) }
    }

    private fun showMealDetailsBottomSheet(meal: Meal) {
        val sheetBinding = BottomSheetMealDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        val mess = allMesses.firstOrNull { it.messId == meal.messId }
        val messInfo = if (mess == null) "" else "From ${mess.name}, ${mess.city}"

        sheetBinding.ivMeal.load(meal.imageUrl)
        sheetBinding.tvMealName.text = meal.name
        sheetBinding.tvMealMeta.text = "Rs ${meal.price}" + if (messInfo.isBlank()) "" else " | $messInfo"
        sheetBinding.tvMealDescription.text = meal.description.ifBlank { "No description available" }

        sheetBinding.btnPlaceOrder.setOnClickListener {
            val qty = sheetBinding.etQuantity.text?.toString()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val instructions = sheetBinding.etInstructions.text?.toString()?.trim().orEmpty().ifBlank { null }
            viewModel.orderMeal(meal, qty, instructions)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun applyCloudSearch() {
        val query = binding.etSearchMeals.text?.toString().orEmpty().trim().lowercase()
        if (query.isBlank()) {
            cloudAdapter.submitList(allCloudMeals)
            return
        }
        val filtered = allCloudMeals.filter { meal ->
            val mess = allMesses.firstOrNull { it.messId == meal.messId }
            meal.name.lowercase().contains(query) ||
                meal.description.lowercase().contains(query) ||
                mess?.name?.lowercase()?.contains(query) == true ||
                mess?.city?.lowercase()?.contains(query) == true
        }
        cloudAdapter.submitList(filtered)
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.cloudMealsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading cloud meals..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                allCloudMeals = state.data
                                applyCloudSearch()
                                binding.tvInfo.text = if (state.data.isEmpty()) "No cloud meals available" else "Cloud meals: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.messesState.collect { state ->
                        if (state is Resource.Success) {
                            allMesses = state.data
                        }
                    }
                }

                launch {
                    viewModel.bannerState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                bannerAdapter.submitList(state.data)
                                binding.tvBannerEmpty.text = if (state.data.isEmpty()) "No active banners" else ""
                            }
                            is Resource.Error -> binding.tvBannerEmpty.text = state.message
                            Resource.Loading -> Unit
                        }
                    }
                }

                launch {
                    viewModel.enrolledMessState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.tvMessTitle.text = "Mess Section"
                                binding.tvMessDetails.text = state.message
                            }
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                val mess = state.data
                                if (mess == null) {
                                    binding.tvMessTitle.text = "Not enrolled in any mess"
                                    binding.tvMessDetails.text = "Browse messes and send a join request"
                                    binding.rvMessMeals.visibility = View.GONE
                                    binding.tvMessMealsEmpty.visibility = View.VISIBLE
                                    binding.tvMessMealsEmpty.text = "Join a mess to view menu"
                                } else {
                                    binding.tvMessTitle.text = mess.name
                                    binding.tvMessDetails.text = "${mess.address}, ${mess.city}"
                                    binding.rvMessMeals.visibility = View.VISIBLE
                                    binding.tvMessMealsEmpty.text = "No menu available for your mess"
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.messMealsState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                messMeals = state.data
                                messMealAdapter.submitList(state.data)
                                binding.tvMessMealsEmpty.visibility = if (state.data.isEmpty()) View.VISIBLE else View.GONE
                            }
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                        }
                    }
                }


                launch {
                    viewModel.paymentsState.collect { state ->
                        if (state is Resource.Success) {
                            val messBills = state.data.filter { it.category() == "mess_bill" }
                            val pendingCount = messBills.count { it.status == "pending" || it.status == "payment_submitted" }
                            val pendingAmount = messBills
                                .filter { it.status == "pending" || it.status == "payment_submitted" }
                                .sumOf { it.amount }
                            val paidCount = messBills.count { it.status == "paid" }
                            binding.tvPaymentSummary.text =
                                "Monthly bills - Pending: $pendingCount (Rs $pendingAmount) | Paid: $paidCount"
                        }
                    }
                }

                launch {
                    viewModel.orderState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Order placed: ${state.data}", Toast.LENGTH_SHORT).show()
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

