package com.example.mymess.core

import android.content.Context
import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class CloudinaryImageUploader @Inject constructor(
    @ApplicationContext private val context: Context
) : ImageUploader {

    init {
        try {
            val config = mapOf(
                "cloud_name" to AppConstants.CLOUDINARY_CLOUD_NAME,
                "api_key" to AppConstants.CLOUDINARY_API_KEY,
                "api_secret" to AppConstants.CLOUDINARY_API_SECRET
            )
            MediaManager.init(context, config)
        } catch (ignored: Exception) {
            // MediaManager might already be initialized
        }
    }

    override suspend fun uploadImage(imageUri: Uri): Resource<String> = suspendCancellableCoroutine { continuation ->
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        continuation.resume(Resource.Success(url))
                    } else {
                        continuation.resume(Resource.Error("Failed to get secure URL from Cloudinary"))
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(Resource.Error(error?.description ?: "Unknown error during upload"))
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    continuation.resume(Resource.Error("Upload rescheduled: ${error?.description}"))
                }
            })
            .dispatch()
    }
}
