package com.example.mymess.core

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val sharedPreferences: SharedPreferences,
) {
    fun saveSession(uid: String, role: String) {
        sharedPreferences.edit()
            .putString(AppConstants.PREF_UID, uid)
            .putString(AppConstants.PREF_ROLE, role)
            .apply()
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun getUid(): String? = sharedPreferences.getString(AppConstants.PREF_UID, null)

    fun getRole(): String? = sharedPreferences.getString(AppConstants.PREF_ROLE, null)

    fun isLoggedIn(): Boolean = !getUid().isNullOrBlank() && !getRole().isNullOrBlank()
}

