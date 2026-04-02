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
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentOwnerPendingOrdersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerPendingOrdersFragment : Fragment() {

    private var _binding: FragmentOwnerPendingOrdersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerPendingOrdersViewModel by viewModels()
    private val adapter = OwnerPendingOrdersAdapter { viewModel.advance(it) }

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
        observe()
        viewModel.load()
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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.ordersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading pending orders..."
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Orders in progress: ${state.data.size}"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Order status updated", Toast.LENGTH_SHORT).show()
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
