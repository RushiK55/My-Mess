package com.example.mymess.presentation.owner

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.MainActivity
import com.example.mymess.core.Resource
import com.example.mymess.data.models.OwnerUserBillingDetails
import com.example.mymess.data.models.User
import com.example.mymess.databinding.BottomSheetOwnerUserDetailsBinding
import com.example.mymess.databinding.FragmentOwnerUsersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class OwnerUsersFragment : Fragment() {

    private var _binding: FragmentOwnerUsersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerUsersViewModel by viewModels()
    private val adapter = OwnerUsersAdapter { showUserDetails(it) }
    private var allUsers: List<User> = emptyList()
    private var pendingUserForDetails: User? = null
    private var selectedUserForBillingAction: User? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        binding.etSearch.addTextChangedListener { applySearch(it?.toString().orEmpty()) }
        observe()
        viewModel.loadUsers()
    }

    private fun observe() {
        val mainActivity = activity as? MainActivity
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.usersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                mainActivity?.showLoader("Loading enrolled users...")
                                binding.tvInfo.text = "Loading enrolled users..."
                            }
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                allUsers = state.data
                                adapter.submitList(state.data)
                                binding.tvInfo.text = "Enrolled users: ${state.data.size}"
                            }
                        }
                    }
                }

                launch {
                    viewModel.billingDetailsState.collect { state ->
                        when (state) {
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                val user = pendingUserForDetails
                                if (user != null) {
                                    showUserDetailsSheet(user, state.data)
                                    pendingUserForDetails = null
                                }
                            }
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                val user = pendingUserForDetails
                                if (user != null) {
                                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                    pendingUserForDetails = null
                                }
                            }
                            Resource.Loading -> mainActivity?.showLoader("Loading billing info...")
                            null -> Unit
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
                            Resource.Loading -> mainActivity?.showLoader("Updating user status...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "User status updated", Toast.LENGTH_SHORT).show()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.billGenerationState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            Resource.Loading -> mainActivity?.showLoader("Generating bill...")
                            is Resource.Success -> {
                                mainActivity?.hideLoader()
                                Toast.makeText(requireContext(), "Bill generated", Toast.LENGTH_SHORT).show()
                                val user = selectedUserForBillingAction
                                if (user != null) {
                                    pendingUserForDetails = user
                                    viewModel.loadBillingDetails(user.uid)
                                }
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun applySearch(raw: String) {
        val query = raw.trim().lowercase()
        val filtered = if (query.isBlank()) allUsers else allUsers.filter {
            it.name.lowercase().contains(query) || it.email.lowercase().contains(query)
        }
        adapter.submitList(filtered)
    }

    private fun showUserDetails(user: User) {
        pendingUserForDetails = user
        selectedUserForBillingAction = user
        viewModel.loadBillingDetails(user.uid)
    }

    private fun showUserDetailsSheet(user: User, details: OwnerUserBillingDetails) {
        val sheetBinding = BottomSheetOwnerUserDetailsBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)

        sheetBinding.tvUserName.text = user.name
        sheetBinding.tvUserEmail.text = user.email
        sheetBinding.tvUserPhone.text = user.phone.ifBlank { "No phone number" }
        
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sheetBinding.tvJoinDate.text = "Joined on ${sdf.format(Date(user.createdAt))}"

        val preview = details.billPreview
        sheetBinding.tvBillPeriod.text = "Period: ${preview.periodLabel}"
        
        if (preview.items.isEmpty()) {
            sheetBinding.tvBillDetails.text = "No accepted mess meals in this period."
        } else {
            val detailsText = preview.items.joinToString("\n") { 
                "${it.mealName} x${it.quantity} = Rs ${it.amount}" 
            }
            sheetBinding.tvBillDetails.text = detailsText
        }
        sheetBinding.tvBillTotal.text = "Total: Rs ${preview.totalAmount}"

        sheetBinding.btnGenerateBill.visibility = if (preview.canGenerate) View.VISIBLE else View.GONE
        sheetBinding.btnGenerateBill.setOnClickListener {
            selectedUserForBillingAction = user
            viewModel.generateBillForUser(user.uid)
            dialog.dismiss()
        }

        sheetBinding.btnBlock.text = if (user.status == "blocked") "Unblock User" else "Block User"
        sheetBinding.btnBlock.setOnClickListener {
            if (user.status == "blocked") {
                viewModel.blockUser(user.uid, "") // Assuming empty reason means unblock or handled in VM
            } else {
                askBlockReason(user)
            }
            dialog.dismiss()
        }

        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun askBlockReason(user: User) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Block ${user.name}")
            .setView(input)
            .setMessage("Enter reason for blocking")
            .setPositiveButton("Confirm") { _, _ ->
                viewModel.blockUser(user.uid, input.text?.toString().orEmpty())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
