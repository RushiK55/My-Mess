package com.example.mymess.core

import android.net.Uri

interface ImageUploader {
    suspend fun uploadImage(imageUri: Uri): Resource<String>
}
