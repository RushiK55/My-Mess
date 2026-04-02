package com.example.mymess.presentation.owner

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class OwnerMealsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            OwnerMessMealsFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(OwnerMessMealsFragment.ARG_EMBEDDED, true)
                }
            }
        } else {
            OwnerCloudMealsFragment()
        }
    }
}

