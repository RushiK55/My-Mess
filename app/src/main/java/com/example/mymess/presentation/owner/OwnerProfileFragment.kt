package com.example.mymess.presentation.owner

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
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
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User
import com.example.mymess.databinding.FragmentOwnerProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OwnerProfileFragment : Fragment() {

    private var _binding: FragmentOwnerProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: OwnerProfileViewModel by viewModels()
    private var currentMess: Mess? = null
    private var currentOwner: User? = null
    
    private var loadingDialog: AlertDialog? = null

    private val pickMessMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.updateMessImage(uri)
        }
    }

    private val pickProfileMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.updateOwnerProfilePic(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomNav()
        setupExpandableSections()
        setupQuickActions()
        setupImagePickers()

        binding.btnSaveMess.setOnClickListener {
            val existing = currentMess ?: return@setOnClickListener
            val updated = existing.copy(
                name = binding.etName.text?.toString().orEmpty(),
                address = binding.etAddress.text?.toString().orEmpty(),
                city = binding.etCity.text?.toString().orEmpty(),
                contact = binding.etContact.text?.toString().orEmpty(),
                description = binding.etDescription.text?.toString().orEmpty(),
            )
            viewModel.saveMessDetails(updated)
        }

        binding.btnSaveOwnerProfile.setOnClickListener {
            viewModel.saveOwnerDetails(
                name = binding.etOwnerName.text?.toString().orEmpty(),
                phone = binding.etOwnerPhone.text?.toString().orEmpty()
            )
        }

        binding.btnChangePassword.setOnClickListener {
            viewModel.changePassword(
                currentPassword = binding.etCurrentPassword.text?.toString().orEmpty(),
                newPassword = binding.etNewPassword.text?.toString().orEmpty(),
                confirmPassword = binding.etConfirmNewPassword.text?.toString().orEmpty(),
            )
        }
        binding.btnLogout.setOnClickListener { showLogoutConfirmDialog() }
        observe()
        viewModel.loadMess()
    }

    private fun showLoading(show: Boolean, message: String = "Please wait...") {
        if (show) {
            if (loadingDialog == null) {
                loadingDialog = AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setView(R.layout.layout_loading_dialog)
                    .create()
            }
            loadingDialog?.show()
        } else {
            loadingDialog?.dismiss()
        }
    }

    private fun setupImagePickers() {
        binding.cardMessImagePicker.setOnClickListener {
            pickMessMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.fabEditProfilePic.setOnClickListener {
            pickProfileMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    private fun setupExpandableSections() {
        binding.layoutEditMessHeader.setOnClickListener {
            toggleSection(binding.layoutMessFields, binding.ivMessArrow)
        }
        binding.layoutEditOwnerHeader.setOnClickListener {
            toggleSection(binding.layoutOwnerFields, binding.ivOwnerArrow)
        }
        binding.layoutPasswordHeader.setOnClickListener {
            toggleSection(binding.layoutPasswordFields, binding.ivPasswordArrow)
        }
    }

    private fun toggleSection(view: View, arrow: View) {
        val show = view.visibility == View.GONE
        TransitionManager.beginDelayedTransition(binding.profileContainer, AutoTransition())
        view.visibility = if (show) View.VISIBLE else View.GONE
        arrow.animate().rotation(if (show) 90f else 0f).start()
    }

    private fun setupBottomNav() {
        binding.bottomNavOwner.selectedItemId = R.id.nav_owner_profile
        binding.bottomNavOwner.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_owner_home -> {
                    findNavController().navigate(R.id.ownerHomeFragment)
                    true
                }
                R.id.nav_owner_orders -> {
                    findNavController().navigate(R.id.ownerPendingOrdersFragment)
                    true
                }
                R.id.nav_owner_meals -> {
                    findNavController().navigate(R.id.ownerMealsFragment)
                    true
                }
                R.id.nav_owner_profile -> true
                else -> false
            }
        }
    }

    private fun setupQuickActions() {
        binding.layoutQuickPayments.setOnClickListener {
            findNavController().navigate(R.id.ownerPaymentsFragment)
        }
        binding.layoutQuickUsers.setOnClickListener {
            findNavController().navigate(R.id.ownerEnrolledUsersFragment)
        }
        binding.layoutQuickBanners.setOnClickListener {
            findNavController().navigate(R.id.ownerBannersFragment)
        }
        binding.layoutQuickAnalytics.setOnClickListener {
            findNavController().navigate(R.id.ownerAnalyticsFragment)
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

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                binding.progressBar.visibility = View.GONE
                                binding.tvInfo.text = state.message
                            }
                            Resource.Loading -> {
                                binding.progressBar.visibility = View.VISIBLE
                                binding.tvInfo.text = "Loading mess profile..."
                            }
                            is Resource.Success -> {
                                binding.progressBar.visibility = View.GONE
                                currentMess = state.data
                                binding.tvMessName.text = state.data.name
                                binding.etName.setText(state.data.name)
                                binding.etAddress.setText(state.data.address)
                                binding.etCity.setText(state.data.city)
                                binding.etContact.setText(state.data.contact)
                                binding.etDescription.setText(state.data.description)

                                if (!state.data.imageUrl.isNullOrBlank()) {
                                    binding.ivMessPreview.visibility = View.VISIBLE
                                    binding.llMessPlaceholder.visibility = View.GONE
                                    binding.ivMessPreview.load(state.data.imageUrl)
                                } else {
                                    binding.ivMessPreview.visibility = View.GONE
                                    binding.llMessPlaceholder.visibility = View.VISIBLE
                                }

                                binding.tvInfo.text = ""
                            }
                        }
                    }
                }

                launch {
                    viewModel.ownerState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                if (binding.tvInfo.text.isBlank()) {
                                    binding.tvInfo.text = state.message
                                }
                            }

                            Resource.Loading -> Unit
                            is Resource.Success -> {
                                currentOwner = state.data
                                binding.tvProfileName.text = state.data.name
                                binding.etOwnerName.setText(state.data.name)
                                binding.etOwnerEmail.setText(state.data.email)
                                binding.etOwnerPhone.setText(state.data.phone)

                                binding.ivProfilePic.load(state.data.profilePic) {
                                    placeholder(R.color.admin_divider)
                                    error(R.color.admin_divider)
                                    transformations(CircleCropTransformation())
                                }

                                when (state.data.status.lowercase()) {
                                    "pending" -> {
                                        binding.tvVerificationStatus.visibility = View.VISIBLE
                                        binding.tvVerificationStatus.text = "PENDING VERIFICATION"
                                    }
                                    "approved" -> {
                                        binding.tvVerificationStatus.visibility = View.GONE
                                    }
                                    "rejected" -> {
                                        binding.tvVerificationStatus.visibility = View.VISIBLE
                                        binding.tvVerificationStatus.text = "ACCOUNT REJECTED"
                                    }
                                    "blocked" -> {
                                        binding.tvVerificationStatus.visibility = View.VISIBLE
                                        binding.tvVerificationStatus.text = "ACCOUNT BLOCKED"
                                    }
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.saveState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearSaveState()
                            }
                            Resource.Loading -> showLoading(true, "Updating mess profile...")
                            is Resource.Success -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Mess profile updated", Toast.LENGTH_SHORT).show()
                                viewModel.clearSaveState()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.profileSaveState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileSaveState()
                            }
                            Resource.Loading -> showLoading(true, "Updating owner profile...")
                            is Resource.Success -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Owner profile updated", Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileSaveState()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.passwordState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearPasswordState()
                            }
                            Resource.Loading -> showLoading(true, "Changing password...")
                            is Resource.Success -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                                binding.etCurrentPassword.text = null
                                binding.etNewPassword.text = null
                                binding.etConfirmNewPassword.text = null
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
        loadingDialog?.dismiss()
        _binding = null
    }
}
