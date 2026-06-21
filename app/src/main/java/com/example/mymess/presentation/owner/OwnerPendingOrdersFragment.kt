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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.MainActivity
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Order
import com.example.mymess.databinding.BottomSheetOrderStatusDetailsBinding
import com.example.mymess.databinding.FragmentOwnerPendingOrdersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerPendingOrdersFragment : Fragment() {

    private var _binding: FragmentOwnerPendingOrdersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerPendingOrdersViewModel by viewModels()
    private val adapter = OwnerPendingOrdersAdapter { showOrderDetails(it) }
    
    private var allOrders: List<Order> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerPendingOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        setupBottomNav()
        setupFilters()
        observe()
        viewModel.load()
    }

    private fun setupFilters() {
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            applyFilters()
        }
    }

    private fun applyFilters() {
        val selectedStatus = when (binding.chipGroupStatus.checkedChipId) {
            R.id.chipAccepted -> "accepted"
            R.id.chipPreparing -> "preparing"
            R.id.chipReady -> "ready"
            R.id.chipDelivered -> "delivered"
            else -> "all"
        }

        val filtered = if (selectedStatus == "all") {
            allOrders
        } else {
            allOrders.filter { it.status == selectedStatus }
        }
        adapter.submitList(filtered)
        binding.tvInfo.text = "Showing ${filtered.size} orders"
    }

    private fun showOrderDetails(order: Order) {
        val sheetBinding = BottomSheetOrderStatusDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetMealName.text = order.mealName
        sheetBinding.tvSheetOrderMeta.text = "Qty: ${order.quantity} | Total: Rs ${order.totalPrice}"
        sheetBinding.tvSheetStatus.text = "Current Status: ${order.status.replaceFirstChar { it.uppercase() }}"
        sheetBinding.tvSheetInstructions.text = order.specialInstructions?.takeIf { it.isNotBlank() } ?: "No special instructions"

        sheetBinding.btnSheetAction.text = when (order.status) {
            "accepted" -> "Start Preparing"
            "preparing" -> "Mark as Ready"
            "ready" -> "Deliver Order"
            else -> "Order Delivered"
        }
        sheetBinding.btnSheetAction.isEnabled = order.status != "delivered"

        sheetBinding.btnSheetAction.setOnClickListener {
            viewModel.advance(order)
            dialog.dismiss()
        }

        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun setupBottomNav() {
        binding.bottomNavOwner.selectedItemId = R.id.nav_owner_orders
        binding.bottomNavOwner.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_owner_home -> {
                    findNavController().navigate(R.id.ownerHomeFragment)
                    true
                }
                R.id.nav_owner_orders -> true
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

    private fun observe() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                mainActivity?.showLoader("Loading orders...")
                                binding.tvInfo.text = "Loading pending orders..."
                            }
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                allOrders = state.data
                                applyFilters()
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
                            Resource.Loading -> mainActivity?.showLoader("Updating status...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "Order status updated", Toast.LENGTH_SHORT).show()
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
