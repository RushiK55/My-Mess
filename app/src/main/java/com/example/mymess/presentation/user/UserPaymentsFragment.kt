package com.example.mymess.presentation.user

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.core.Resource
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.databinding.FragmentUserPaymentsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserPaymentsFragment : Fragment() {

    private var _binding: FragmentUserPaymentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserPaymentsViewModel by viewModels()
    private val adapter = UserPaymentsAdapter { payment -> showPayDialog(payment) }
    private var allPayments: List<PaymentRecord> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        binding.rvPayments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPayments.adapter = adapter

        val statusOptions = listOf("all", "pending", "payment_submitted", "paid")
        binding.actStatusFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusOptions))
        binding.actStatusFilter.setText("all", false)
        binding.actStatusFilter.setOnClickListener { binding.actStatusFilter.showDropDown() }
        binding.actStatusFilter.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        binding.btnRefresh.setOnClickListener { viewModel.loadPayments() }
        observeState()
        viewModel.loadPayments()
    }

    private fun showPayDialog(payment: PaymentRecord) {
        val methods = arrayOf("Razorpay", "Stripe", "Cash")
        AlertDialog.Builder(requireContext())
            .setTitle("Select payment method")
            .setItems(methods) { _, which ->
                viewModel.submitPayment(payment.paymentId, methods[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun applyFilters() {
        val selectedStatus = binding.actStatusFilter.text?.toString().orEmpty().ifBlank { "all" }
        val filtered = if (selectedStatus == "all") allPayments else allPayments.filter { it.status == selectedStatus }
        adapter.submitList(filtered)
        binding.tvInfo.text = "Payments: ${filtered.size}"
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.paymentsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading payments..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                allPayments = state.data
                                applyFilters()
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Payment submitted. Awaiting owner confirmation.", Toast.LENGTH_SHORT).show()
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
