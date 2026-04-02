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
import com.example.mymess.core.Resource
import com.example.mymess.data.models.OwnerUserBillingDetails
import com.example.mymess.data.models.User
import com.example.mymess.data.models.category
import com.example.mymess.databinding.FragmentOwnerUsersBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.usersState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading enrolled users..."
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
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
                                val user = pendingUserForDetails
                                if (user != null) {
                                    showDetailsDialog(user, state.data)
                                    pendingUserForDetails = null
                                }
                            }
                            is Resource.Error -> {
                                val user = pendingUserForDetails
                                if (user != null) {
                                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                    pendingUserForDetails = null
                                }
                            }
                            Resource.Loading, null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> Toast.makeText(requireContext(), "User blocked", Toast.LENGTH_SHORT).show()
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.billGenerationState.collect { state ->
                        when (state) {
                            is Resource.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            Resource.Loading -> Unit
                            is Resource.Success -> {
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

    private fun showDetailsDialog(user: User, details: OwnerUserBillingDetails) {
        val preview = details.billPreview
        val mealsText = if (preview.items.isEmpty()) {
            "No accepted mess meals in ${preview.periodLabel}"
        } else {
            preview.items.joinToString("\n") { "${it.mealName} x${it.quantity} = Rs ${it.amount}" }
        }

        val paymentText = if (details.paymentHistory.isEmpty()) {
            "No payment history"
        } else {
            details.paymentHistory.take(5).joinToString("\n") {
                val kind = if (it.category() == "mess_bill") "Bill" else "Cloud"
                "$kind | Rs ${it.amount} - ${it.status}"
            }
        }
        val joinDate = java.text.SimpleDateFormat("dd MMM yyyy").format(java.util.Date(user.createdAt))
        val info = buildString {
            append("Name: ${user.name}\n")
            append("Phone: ${user.phone}\n")
            append("Email: ${user.email}\n")
            append("Join Date: $joinDate\n\n")
            append("Bill Preview (${preview.periodLabel}):\n")
            append(mealsText)
            append("\n\nTotal: Rs ${preview.totalAmount}\n")
            append(if (preview.alreadyGenerated) "Status: Bill already generated" else "Status: Bill not generated")
            append("\n\nPayment History:\n$paymentText")
            if (!user.blockReason.isNullOrBlank()) append("\n\nBlock reason: ${user.blockReason}")
        }

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("User Details")
            .setMessage(info)
            .setPositiveButton("Close", null)
            .setNegativeButton("Block") { _, _ -> askBlockReason(user) }

        if (preview.canGenerate) {
            builder.setNeutralButton("Generate Bill") { _, _ ->
                selectedUserForBillingAction = user
                viewModel.generateBillForUser(user.uid)
            }
        }

        builder.show()
    }

    private fun askBlockReason(user: User) {
        val input = EditText(requireContext())
        AlertDialog.Builder(requireContext())
            .setTitle("Block ${user.name}")
            .setView(input)
            .setMessage("Enter reason")
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
