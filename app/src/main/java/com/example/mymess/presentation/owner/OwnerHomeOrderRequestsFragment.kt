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
import com.example.mymess.data.models.Order
import com.example.mymess.databinding.BottomSheetOrderRequestDetailsBinding
import com.example.mymess.databinding.FragmentOwnerHomeOrderRequestsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerHomeOrderRequestsFragment : Fragment() {

    private var _binding: FragmentOwnerHomeOrderRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerViewModel by viewModels(ownerProducer = { requireParentFragment() })

    private lateinit var adapter: OwnerOrderRequestsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerHomeOrderRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = OwnerOrderRequestsAdapter { order -> showOrderDetails(order) }

        binding.rvOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvOrders.adapter = adapter
        observeUi()
    }

    private fun showOrderDetails(order: Order) {
        val sheetBinding = BottomSheetOrderRequestDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvSheetMealName.text = order.mealName
        sheetBinding.tvSheetOrderMeta.text = "Qty: ${order.quantity} | Total: Rs ${order.totalPrice}"
        sheetBinding.tvSheetInstructions.text = order.specialInstructions?.takeIf { it.isNotBlank() } ?: "No special instructions"

        sheetBinding.btnSheetAccept.setOnClickListener {
            viewModel.acceptOrder(order)
            dialog.dismiss()
        }

        sheetBinding.btnSheetReject.setOnClickListener {
            viewModel.rejectOrder(order)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeUi() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.orderRequestsState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading order requests..."
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Pending orders: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.updateState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Updating order...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "Order updated", Toast.LENGTH_SHORT).show()
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
