package com.example.mymess.presentation.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.MainActivity
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.source
import com.example.mymess.databinding.BottomSheetOrderDetailsBinding
import com.example.mymess.databinding.FragmentUserOrdersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserOrdersFragment : Fragment() {

    private var _binding: FragmentUserOrdersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserHomeViewModel by viewModels()
    private val adapter = OrderHistoryAdapter { order -> showOrderDetails(order) }
    private var allOrders: List<Order> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        setupBottomNav()
        binding.btnRefreshOrders.setOnClickListener { viewModel.loadOrderHistory() }

        val dateFilters = listOf("all", "day", "week", "month")
        binding.actDateFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dateFilters))
        binding.actDateFilter.setText("all", false)
        binding.actDateFilter.setOnClickListener { binding.actDateFilter.showDropDown() }
        binding.actDateFilter.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        val statusFilters = listOf("all", "pending", "accepted", "preparing", "ready", "delivered", "cancelled")
        binding.actStatusFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusFilters))
        binding.actStatusFilter.setText("all", false)
        binding.actStatusFilter.setOnClickListener { binding.actStatusFilter.showDropDown() }
        binding.actStatusFilter.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        binding.etSearchOrders.addTextChangedListener { applyFilters() }
        observeState()
        viewModel.loadOrderHistory()
    }

    private fun setupBottomNav() {
        binding.bottomNavUser.selectedItemId = R.id.nav_user_orders
        binding.bottomNavUser.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_user_home -> R.id.userHomeFragment
                R.id.nav_user_orders -> R.id.userOrdersFragment
                R.id.nav_user_messes -> R.id.userMessesFragment
                R.id.nav_user_profile -> R.id.userProfileFragment
                else -> return@setOnItemSelectedListener false
            }
            if (findNavController().currentDestination?.id == destination) return@setOnItemSelectedListener true
            findNavController().navigate(destination)
            true
        }
    }

    private fun showOrderDetails(order: Order) {
        val sheetBinding = BottomSheetOrderDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        
        sheetBinding.tvOrderMealName.text = order.mealName
        sheetBinding.tvOrderQuantity.text = order.quantity.toString()
        sheetBinding.tvOrderTotal.text = "Rs ${order.totalPrice}"
        sheetBinding.tvOrderStatus.text = order.status
        sheetBinding.tvOrderPayment.text = order.paymentStatus.replaceFirstChar { it.uppercase() }
        sheetBinding.tvOrderInstructions.text = order.specialInstructions?.takeIf { it.isNotBlank() } ?: "No special instructions"
        sheetBinding.tvOrderDate.text = "Placed at: ${format.format(Date(order.createdAt))}"

        val source = order.source()
        sheetBinding.chipOrderSource.text = source.replaceFirstChar { it.uppercase() }
        if (source.equals("cloud", ignoreCase = true)) {
            sheetBinding.chipOrderSource.setChipBackgroundColorResource(R.color.admin_divider)
            sheetBinding.chipOrderSource.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_primary))
        } else {
            sheetBinding.chipOrderSource.setChipBackgroundColorResource(R.color.admin_surface_alt)
            sheetBinding.chipOrderSource.setTextColor(ContextCompat.getColor(requireContext(), R.color.admin_text_secondary))
        }

        val statusColor = when (order.status.lowercase()) {
            "delivered", "ready" -> ContextCompat.getColor(requireContext(), R.color.admin_success)
            "cancelled" -> ContextCompat.getColor(requireContext(), R.color.admin_error)
            "pending" -> ContextCompat.getColor(requireContext(), R.color.admin_warning)
            else -> ContextCompat.getColor(requireContext(), R.color.admin_primary)
        }
        sheetBinding.tvOrderStatus.setTextColor(statusColor)

        sheetBinding.btnCloseSheet.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun applyFilters() {
        val query = binding.etSearchOrders.text?.toString().orEmpty().trim().lowercase()
        val dateFilter = binding.actDateFilter.text?.toString().orEmpty().ifBlank { "all" }
        val statusFilter = binding.actStatusFilter.text?.toString().orEmpty().ifBlank { "all" }

        val now = System.currentTimeMillis()
        val filtered = allOrders.filter { order ->
            val searchOk = query.isBlank() || order.mealName.lowercase().contains(query) || order.status.lowercase().contains(query)
            val statusOk = statusFilter == "all" || order.status == statusFilter
            val dateOk = when (dateFilter) {
                "day" -> now - order.createdAt <= 24 * 60 * 60 * 1000L
                "week" -> now - order.createdAt <= 7 * 24 * 60 * 60 * 1000L
                "month" -> now - order.createdAt <= 30L * 24 * 60 * 60 * 1000
                else -> true
            }
            searchOk && statusOk && dateOk
        }
        adapter.submitList(filtered)
        binding.tvInfo.text = if (filtered.isEmpty()) "No matching orders" else "Total orders: ${filtered.size}"
    }

    private fun observeState() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyState.collect { state ->
                    when (state) {
                        is Resource.Error -> {
                            mainActivity?.hideLoader()
                            binding.tvInfo.text = state.message
                        }

                        Resource.Loading -> {
                            mainActivity?.showLoader("Loading order history...")
                            binding.tvInfo.text = "Loading orders..."
                        }

                        is Resource.Success -> {
                            mainActivity?.hideLoader()
                            allOrders = state.data
                            applyFilters()
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
