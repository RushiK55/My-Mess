package com.example.mymess.presentation.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymess.core.Resource
import com.example.mymess.data.models.User
import com.example.mymess.databinding.BottomSheetAdminUserActionsBinding
import com.example.mymess.databinding.FragmentAdminUsersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AdminUsersFragment : Fragment() {

    private var _binding: FragmentAdminUsersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AdminUsersViewModel by viewModels()
    private val adapter = AdminUsersAdapter(
        onOpenDetails = { user -> showUserDetails(user) },
    )
    private var allUsers: List<User> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAdminUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
        binding.btnRefresh.setOnClickListener { viewModel.loadUsers() }
        binding.etSearchUsers.addTextChangedListener { applyFilter(it?.toString()) }
        observeUi()
        viewModel.loadUsers()
    }

    private fun applyFilter(raw: String?) {
        val query = raw.orEmpty().trim().lowercase()
        val filtered = if (query.isBlank()) {
            allUsers
        } else {
            allUsers.filter {
                it.name.lowercase().contains(query) || it.email.lowercase().contains(query)
            }
        }
        adapter.submitList(filtered)
    }

    private fun confirmToggle(user: User) {
        val action = if (user.status == "blocked") "Unblock" else "Block"
        AlertDialog.Builder(requireContext())
            .setTitle("$action user")
            .setMessage("Are you sure you want to ${action.lowercase()} ${user.name}?")
            .setPositiveButton(action) { _, _ -> viewModel.toggleBlock(user) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(user: User) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete user")
            .setMessage("Delete ${user.name}? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteUser(user) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showUserDetails(user: User) {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetAdminUserActionsBinding.inflate(layoutInflater)
        val actionLabel = if (user.status == "blocked") "Unblock user" else "Block user"

        sheetBinding.tvName.text = user.name
        sheetBinding.tvEmail.text = "Email: ${user.email}"
        sheetBinding.tvPhone.text = "Phone: ${if (user.phone.isBlank()) "-" else user.phone}"
        sheetBinding.tvRole.text = "Role: ${user.role.replaceFirstChar { it.uppercase() }}"
        sheetBinding.tvStatus.text = "Status: ${user.status.replaceFirstChar { it.uppercase() }}"
        sheetBinding.btnBlockUnblock.text = actionLabel

        sheetBinding.btnBlockUnblock.setOnClickListener {
            dialog.dismiss()
            confirmToggle(user)
        }
        sheetBinding.btnDeleteUser.setOnClickListener {
            dialog.dismiss()
            confirmDelete(user)
        }
        sheetBinding.btnClose.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun observeUi() {
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
                                binding.tvInfo.text = "Loading users..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                allUsers = state.data
                                applyFilter(binding.etSearchUsers.text?.toString())
                                binding.tvInfo.text = "Total users: ${state.data.size}"
                            }
                        }
                    }
                }
                launch {
                    viewModel.actionState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.tvInfo.text = state.message
                                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                            }
                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                binding.tvInfo.text = state.data
                                Snackbar.make(binding.root, state.data, Snackbar.LENGTH_SHORT).show()
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


