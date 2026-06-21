package com.example.mymess.presentation.user

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.CircleCropTransformation
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.User
import com.example.mymess.databinding.FragmentUserProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private var _binding: FragmentUserProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserProfileViewModel by viewModels()
    private var currentUser: User? = null
    private var selectedProfilePicUri: Uri? = null

    private val pickProfileMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedProfilePicUri = uri
            binding.ivProfilePic.load(uri) {
                transformations(CircleCropTransformation())
            }
        }
    }

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

        setupSectionToggles()
        setupQuickAccess()
        setupBottomNav()
        setupImagePicker()

        binding.btnSaveProfile.setOnClickListener {
            viewModel.updateProfile(
                name = binding.etName.text?.toString().orEmpty(),
                phone = binding.etPhone.text?.toString().orEmpty(),
                profilePicUri = selectedProfilePicUri,
                existingProfilePic = currentUser?.profilePic
            )
        }

        binding.btnChangePassword.setOnClickListener {
            viewModel.changePassword(
                currentPassword = binding.etCurrentPassword.text?.toString().orEmpty(),
                newPassword = binding.etNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmPassword.text?.toString().orEmpty(),
            )
        }

        binding.btnLogout.setOnClickListener { showLogoutConfirmDialog() }

        observeUi()
        viewModel.loadProfile()
    }

    private fun setupImagePicker() {
        binding.fabEditProfilePic.setOnClickListener {
            pickProfileMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun setupSectionToggles() {
        binding.layoutEditProfileHeader.setOnClickListener {
            toggleSection(binding.layoutEditProfileFields, binding.ivEditProfileArrow)
        }

        binding.layoutPasswordHeader.setOnClickListener {
            toggleSection(binding.layoutPasswordFields, binding.ivPasswordArrow)
        }
    }

    private fun toggleSection(section: View, arrow: View) {
        val isVisible = section.visibility == View.VISIBLE
        TransitionManager.beginDelayedTransition(binding.root as ViewGroup, AutoTransition())
        section.visibility = if (isVisible) View.GONE else View.VISIBLE
        arrow.animate().rotation(if (isVisible) 0f else 90f).setDuration(200).start()
    }

    private fun setupQuickAccess() {
        binding.layoutQuickOrders.setOnClickListener {
            findNavController().navigate(R.id.userOrdersFragment)
        }
        binding.layoutQuickPayments.setOnClickListener {
            findNavController().navigate(R.id.userPaymentsFragment)
        }
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
                                val user = state.data
                                currentUser = user
                                
                                // Header
                                binding.tvProfileName.text = user.name
                                binding.tvUserStatus.text = if (user.enrolledMessId != null) "Gold Member" else "Standard Member"
                                
                                // Profile Pic
                                binding.ivProfilePic.load(user.profilePic) {
                                    placeholder(R.color.admin_divider)
                                    error(android.R.drawable.ic_menu_gallery)
                                    transformations(CircleCropTransformation())
                                }

                                // Account Info
                                binding.tvUserId.text = "#${user.uid.takeLast(6).uppercase()}"
                                binding.tvProfileEmail.text = user.email
                                
                                // Edit Fields
                                binding.etName.setText(user.name)
                                binding.etPhone.setText(user.phone)
                            }
                        }
                    }
                }

                launch {
                    viewModel.profileUpdateState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileUpdateState()
                            }

                            Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                                toggleSection(binding.layoutEditProfileFields, binding.ivEditProfileArrow)
                                selectedProfilePicUri = null
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
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearPasswordState()
                            }

                            Resource.Loading -> binding.progressBar.visibility = View.VISIBLE
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                                binding.etCurrentPassword.text = null
                                binding.etNewPassword.text = null
                                binding.etConfirmPassword.text = null
                                toggleSection(binding.layoutPasswordFields, binding.ivPasswordArrow)
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
