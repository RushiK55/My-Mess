package com.example.mymess.di

import com.example.mymess.core.CloudinaryImageUploader
import com.example.mymess.core.ImageUploader
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UploaderModule {

    @Binds
    @Singleton
    abstract fun bindImageUploader(
        cloudinaryImageUploader: CloudinaryImageUploader
    ): ImageUploader
}
