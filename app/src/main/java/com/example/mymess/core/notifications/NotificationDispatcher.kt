package com.example.mymess.core.notifications

import android.content.Context
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationDispatcher @Inject constructor() {

    // Placeholder dispatcher for local feedback; can be replaced with FCM-backed delivery.
    fun dispatch(context: Context, event: NotificationEvent, message: String) {
        Toast.makeText(context, "${event.name}: $message", Toast.LENGTH_SHORT).show()
    }
}

