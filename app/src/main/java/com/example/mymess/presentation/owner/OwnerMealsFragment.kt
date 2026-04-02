package com.example.mymess.presentation.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mymess.R
import com.example.mymess.databinding.FragmentOwnerMealsBinding
import com.google.android.material.tabs.TabLayoutMediator

class OwnerMealsFragment : Fragment() {

    private var _binding: FragmentOwnerMealsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentOwnerMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.vpMeals.adapter = OwnerMealsPagerAdapter(this)
        TabLayoutMediator(binding.tabMeals, binding.vpMeals) { tab, position ->
            tab.text = if (position == 0) "Mess Meals" else "Cloud Meals"
        }.attach()
        setupBottomNav()
    }

    private fun setupBottomNav() {
        binding.bottomNavOwner.selectedItemId = R.id.nav_owner_meals
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
                R.id.nav_owner_meals -> true
                R.id.nav_owner_profile -> {
                    findNavController().navigate(R.id.ownerProfileFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

