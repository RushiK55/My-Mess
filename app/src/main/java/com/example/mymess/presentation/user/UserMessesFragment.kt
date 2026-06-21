package com.example.mymess.presentation.user

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
import coil.load
import com.example.mymess.MainActivity
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.databinding.BottomSheetMessDetailsBinding
import com.example.mymess.databinding.FragmentUserMessesBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
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

    private fun showMessDetailsBottomSheet(mess: Mess, meals: List<Meal>) {
        val sheetBinding = BottomSheetMessDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.ivMessImage.load(mess.imageUrl) {
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_menu_gallery)
        }
        sheetBinding.tvMessName.text = mess.name
        sheetBinding.tvMessLocation.text = "${mess.address}, ${mess.city}"
        sheetBinding.tvMessContact.text = mess.contact
        sheetBinding.tvMessDescription.text = mess.description.ifBlank { "No description available" }

        val menuAdapter = MealAdapter(onMealClick = { /* Can view details here if needed */ })
        sheetBinding.rvMessMenu.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvMessMenu.adapter = menuAdapter
        menuAdapter.submitList(meals)

        sheetBinding.tvMenuEmpty.visibility = if (meals.isEmpty()) View.VISIBLE else View.GONE
        
        sheetBinding.btnJoinMess.setOnClickListener {
            viewModel.requestJoinMess(mess.messId)
            dialog.dismiss()
        }
        
        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun observeUi() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messesState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                mainActivity?.showLoader("Finding messes near you...")
                                binding.tvInfo.text = "Loading approved messes..."
                            }

                            is Resource.Success -> {
                                mainActivity?.hideLoader()
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
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Loading menu...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                selectedMessForDetails?.let { 
                                    showMessDetailsBottomSheet(it, state.data)
                                    selectedMessForDetails = null // Reset after showing
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.joinState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Sending request...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "Join request sent", Toast.LENGTH_SHORT).show()
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
