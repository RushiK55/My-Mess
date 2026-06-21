package com.example.mymess.presentation.auth

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.mymess.R
import com.example.mymess.core.Resource
import com.example.mymess.databinding.FragmentLoginBinding
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private var forgotPasswordDialog: AlertDialog? = null
    private var loadingDialog: AlertDialog? = null
    
    private var selectedProfileImageUri: Uri? = null

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedProfileImageUri = uri
            binding.ivRegProfilePreview.visibility = View.VISIBLE
            binding.llRegProfilePlaceholder.visibility = View.GONE
            binding.ivRegProfilePreview.load(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roles = listOf("user", "owner")
        binding.actRegRole.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roles))
        binding.actRegRole.setText("user", false)
        binding.actRegRole.setOnClickListener { binding.actRegRole.showDropDown() }
        binding.actRegRole.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) binding.actRegRole.showDropDown() }
        binding.actRegRole.setOnItemClickListener { _, _, _, _ ->
            updateOwnerFieldsVisibility(binding.actRegRole.text?.toString().orEmpty())
        }
   
        showLoginMode()
        binding.toggleAuthMode.check(R.id.btnModeLogin)

        binding.btnModeLogin.setOnClickListener { showLoginMode() }
        binding.btnModeRegister.setOnClickListener { showRegisterMode() }

        binding.cardProfileImagePicker.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.etLoginEmail.text.toString().trim(),
                binding.etLoginPassword.text.toString().trim(),
            )
        }
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.btnRegister.setOnClickListener {
            viewModel.register(
                name = binding.etRegName.text.toString().trim(),
                email = binding.etRegEmail.text.toString().trim(),
                phone = binding.etRegPhone.text.toString().trim(),
                password = binding.etRegPassword.text.toString().trim(),
                role = binding.actRegRole.text.toString().ifBlank { "user" },
                profileImageUri = selectedProfileImageUri,
                messName = binding.etRegMessName.text.toString().trim(),
                messAddress = binding.etRegMessAddress.text.toString().trim(),
                messCity = binding.etRegMessCity.text.toString().trim(),
                messContact = binding.etRegMessContact.text.toString().trim(),
                messDescription = binding.etRegMessDescription.text.toString().trim(),
            )
        }

        updateOwnerFieldsVisibility(binding.actRegRole.text?.toString().orEmpty())
        observeAuthState()
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

    private fun showLoginMode() {
        binding.layoutLoginForm.visibility = View.VISIBLE
        binding.layoutRegisterForm.visibility = View.GONE
        binding.toggleAuthMode.check(R.id.btnModeLogin)
    }

    private fun showRegisterMode() {
        binding.layoutLoginForm.visibility = View.GONE
        binding.layoutRegisterForm.visibility = View.VISIBLE
        binding.toggleAuthMode.check(R.id.btnModeRegister)
        updateOwnerFieldsVisibility(binding.actRegRole.text?.toString().orEmpty())
    }

    private fun updateOwnerFieldsVisibility(role: String) {
        binding.layoutOwnerFields.visibility = if (role == "owner") View.VISIBLE else View.GONE
    }

    private fun showForgotPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_forgot_password, null)
        val emailInput = dialogView.findViewById<TextInputEditText>(R.id.etForgotEmail)
        val newPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.etForgotNewPassword)
        val confirmPasswordInput = dialogView.findViewById<TextInputEditText>(R.id.etForgotConfirmPassword)

        emailInput?.setText(binding.etLoginEmail.text?.toString().orEmpty())

        forgotPasswordDialog?.dismiss()
        forgotPasswordDialog = AlertDialog.Builder(requireContext())
            .setTitle("Forgot Password")
            .setView(dialogView)
            .setPositiveButton("Reset", null)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        forgotPasswordDialog?.setOnShowListener {
            forgotPasswordDialog?.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
                viewModel.resetPassword(
                    email = emailInput?.text?.toString().orEmpty(),
                    newPassword = newPasswordInput?.text?.toString().orEmpty(),
                    confirmPassword = confirmPasswordInput?.text?.toString().orEmpty(),
                )
            }
        }
        forgotPasswordDialog?.show()
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.authState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearAuthState()
                            }

                            Resource.Loading -> showLoading(true, "Authenticating...")
                            is Resource.Success -> {
                                showLoading(false)
                                if (state.data.status != "approved") {
                                    Toast.makeText(requireContext(), "Registered. Please wait for approval.", Toast.LENGTH_SHORT).show()
                                    showLoginMode()
                                    viewModel.clearAuthState()
                                    return@collect
                                }
                                val destination = when (state.data.role) {
                                    "owner" -> R.id.action_loginFragment_to_ownerHomeFragment
                                    "admin" -> R.id.action_loginFragment_to_adminHomeFragment
                                    else -> R.id.action_loginFragment_to_userHomeFragment
                                }
                                viewModel.clearAuthState()
                                findNavController().navigate(destination)
                            }

                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.forgotPasswordState.collect { state ->
                        when (state) {
                            is Resource.Error -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                viewModel.clearForgotPasswordState()
                            }
                            Resource.Loading -> showLoading(true, "Resetting password...")
                            is Resource.Success -> {
                                showLoading(false)
                                Toast.makeText(requireContext(), "Password reset successful", Toast.LENGTH_SHORT).show()
                                forgotPasswordDialog?.dismiss()
                                viewModel.clearForgotPasswordState()
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
        forgotPasswordDialog?.dismiss()
        forgotPasswordDialog = null
        loadingDialog?.dismiss()
        _binding = null
    }
}
