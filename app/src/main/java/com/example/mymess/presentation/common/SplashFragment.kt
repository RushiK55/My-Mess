package com.example.mymess.presentation.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mymess.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_splash, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destinationState.collect { destination ->
                    val navTarget = when (destination) {
                        "owner" -> R.id.action_splashFragment_to_ownerHomeFragment
                        "admin" -> R.id.action_splashFragment_to_adminHomeFragment
                        "user" -> R.id.action_splashFragment_to_userHomeFragment
                        "login" -> R.id.action_splashFragment_to_loginFragment
                        else -> null
                    }

                    if (navTarget != null) {
                        viewModel.clearDestination()
                        findNavController().navigate(navTarget)
                    }
                }
            }
        }
        viewModel.resolveDestination()
    }
}

