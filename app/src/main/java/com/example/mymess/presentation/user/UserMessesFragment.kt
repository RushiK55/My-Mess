package com.example.mymess.presentation.user

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.example.mymess.databinding.FragmentUserMessesBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserMessesFragment : Fragment() {

    private var _binding: FragmentUserMessesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserHomeViewModel by viewModels()
    private val adapter = MessBrowseAdapter(
        onRequestJoin = { mess -> viewModel.requestJoinMess(mess.messId) },
        onViewDetails = { mess -> showMessDetails(mess) },
    )
    private var allMesses: List<Mess> = emptyList()
    private var selectedMessForDetails: Mess? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserMessesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvMesses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMesses.adapter = adapter
        setupBottomNav()
        binding.btnRefresh.setOnClickListener { viewModel.loadApprovedMesses() }
        binding.etSearchMesses.addTextChangedListener { text ->
            val query = text?.toString().orEmpty().trim().lowercase()
            val filtered = if (query.isBlank()) {
                allMesses
            } else {
                allMesses.filter {
                    it.name.lowercase().contains(query) ||
                        it.city.lowercase().contains(query) ||
                        it.address.lowercase().contains(query)
                }
            }
            adapter.submitList(filtered)
        }
        observeUi()
        viewModel.loadApprovedMesses()
    }

    private fun setupBottomNav() {
        binding.bottomNavUser.selectedItemId = R.id.nav_user_messes
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

    private fun showMessDetails(mess: Mess) {
        selectedMessForDetails = mess
        viewModel.loadMenuForMess(mess.messId)
    }

    private fun showMessDetailsDialog(mess: Mess, meals: List<Meal>) {
        val menuText = if (meals.isEmpty()) {
            "No meals available right now"
        } else {
            meals.joinToString(separator = "\n") { "- ${it.name}: Rs ${it.price}" }
        }
        AlertDialog.Builder(requireContext())
            .setTitle(mess.name)
            .setMessage("${mess.address}, ${mess.city}\nContact: ${mess.contact}\n\n${mess.description}\n\nMenu:\n$menuText")
            .setPositiveButton("Request to Join") { _, _ -> viewModel.requestJoinMess(mess.messId) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messesState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading approved messes..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                allMesses = state.data
                                adapter.submitList(state.data)
                                binding.tvInfo.text = if (state.data.isEmpty()) {
                                    "No approved messes found"
                                } else {
                                    "Available messes: ${state.data.size}"
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.selectedMessMenuState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                selectedMessForDetails?.let { showMessDetailsDialog(it, state.data) }
                            }
                        }
                    }
                }

                launch {
                    viewModel.joinState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "Join request sent", Toast.LENGTH_SHORT).show()
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
