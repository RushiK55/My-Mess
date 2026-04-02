package com.example.mymess.presentation.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.core.Resource
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.category
import com.example.mymess.databinding.FragmentOwnerPaymentsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerPaymentsFragment : Fragment() {

    private var _binding: FragmentOwnerPaymentsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OwnerPaymentsViewModel by viewModels()
    private val adapter = OwnerPaymentsAdapter { payment -> viewModel.markPaid(payment.paymentId) }
    private var allPayments: List<PaymentRecord> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvPayments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPayments.adapter = adapter

        val statusOptions = listOf("all", "pending", "payment_submitted", "paid")
        binding.actStatusFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusOptions))
        binding.actStatusFilter.setText("all", false)
        binding.actStatusFilter.setOnClickListener { binding.actStatusFilter.showDropDown() }
        binding.actStatusFilter.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        val billStateOptions = listOf("all", "unpaid", "paid")
        binding.actBillStateFilter.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, billStateOptions))
        binding.actBillStateFilter.setText("all", false)
        binding.actBillStateFilter.setOnClickListener { binding.actBillStateFilter.showDropDown() }
        binding.actBillStateFilter.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        binding.btnRefresh.setOnClickListener { viewModel.loadPayments() }
        binding.etSearchPayments.addTextChangedListener { applyFilters() }

        observeState()
        viewModel.loadPayments()
    }

    private fun applyFilters() {
        val query = binding.etSearchPayments.text?.toString().orEmpty().trim().lowercase()
        val statusFilter = binding.actStatusFilter.text?.toString().orEmpty().ifBlank { "all" }
        val billStateFilter = binding.actBillStateFilter.text?.toString().orEmpty().ifBlank { "all" }

        val filtered = allPayments.filter { payment ->
            val statusMatches = statusFilter == "all" || payment.status == statusFilter
            val billStateMatches = when (billStateFilter) {
                "paid" -> payment.category() == "mess_bill" && payment.status == "paid"
                "unpaid" -> payment.category() == "mess_bill" && payment.status != "paid"
                else -> true
            }
            val searchMatches = query.isBlank() ||
                payment.status.lowercase().contains(query) ||
                payment.userId.lowercase().contains(query) ||
                payment.userName?.lowercase()?.contains(query) == true
            statusMatches && billStateMatches && searchMatches
        }
        adapter.submitList(filtered)
        val paidBills = filtered.count { it.category() == "mess_bill" && it.status == "paid" }
        val unpaidBills = filtered.count { it.category() == "mess_bill" && it.status != "paid" }
        binding.tvInfo.text = "Payment records: ${filtered.size} | Bills unpaid: $unpaidBills | Bills paid: $paidBills"
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
                                binding.tvInfo.text = "Loading owner payments..."
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
                            is Resource.Success -> Toast.makeText(requireContext(), "Payment marked paid", Toast.LENGTH_SHORT).show()
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
