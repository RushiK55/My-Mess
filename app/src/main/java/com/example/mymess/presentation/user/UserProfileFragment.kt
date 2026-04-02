package com.example.mymess.presentation.user

import android.app.AlertDialog
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentUserProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSaveProfile.setOnClickListener {
            viewModel.updateProfile(
                name = binding.etName.text?.toString().orEmpty(),
                phone = binding.etPhone.text?.toString().orEmpty(),
            )
        }

        binding.btnChangePassword.setOnClickListener {
            viewModel.changePassword(
                currentPassword = binding.etCurrentPassword.text?.toString().orEmpty(),
                newPassword = binding.etNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
            )
        }

        setupBottomNav()

        binding.chipPayments.setOnClickListener {
            findNavController().navigate(R.id.userPaymentsFragment)
        }

        binding.btnLogout.setOnClickListener { showLogoutConfirmDialog() }

        observeUi()
        viewModel.loadProfile()
    }

    private fun setupBottomNav() {
        binding.bottomNavUser.selectedItemId = R.id.nav_user_profile
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

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> viewModel.logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeUi() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.profileState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }

                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading profile..."
                            }

                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = ""
                                binding.etName.setText(state.data.name)
                                binding.etEmail.setText(state.data.email)
                                binding.etPhone.setText(state.data.phone)
                            }
                        }
                    }
                }

                launch {
                    viewModel.profileUpdateState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileUpdateState()
                            }

                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileUpdateState()
                            }

                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.passwordState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearPasswordState()
                            }

                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                                binding.etCurrentPassword.text = null
                                binding.etNewPassword.text = null
                                binding.etConfirmPassword.text = null
                                viewModel.clearPasswordState()
                            }

                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.logoutState.collect { loggedOut ->
                        if (!loggedOut) return@collect
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                        findNavController().navigate(R.id.loginFragment, null, navOptions)
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

