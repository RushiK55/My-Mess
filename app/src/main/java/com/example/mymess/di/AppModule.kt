package com.example.mymess.di

import android.content.Context
import android.content.SharedPreferences
import com.example.mymess.core.AppConstants
import com.example.mymess.data.repository.AuthRepository
import com.example.mymess.data.repository.AdminRepository
import com.example.mymess.data.repository.AnalyticsRepository
import com.example.mymess.data.repository.BannerRepository
import com.example.mymess.data.repository.MessRepository
import com.example.mymess.data.repository.OwnerRepository
import com.example.mymess.data.repository.PaymentRepository
import com.example.mymess.data.repository.impl.AdminRepositoryImpl
import com.example.mymess.data.repository.impl.AnalyticsRepositoryImpl
import com.example.mymess.data.repository.impl.AuthRepositoryImpl
import com.example.mymess.data.repository.impl.BannerRepositoryImpl
import com.example.mymess.data.repository.impl.MessRepositoryImpl
import com.example.mymess.data.repository.impl.OwnerRepositoryImpl
import com.example.mymess.data.repository.impl.PaymentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    abstract fun bindMessRepository(impl: MessRepositoryImpl): MessRepository

    @Binds
    abstract fun bindOwnerRepository(impl: OwnerRepositoryImpl): OwnerRepository

    @Binds
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    abstract fun bindPaymentRepository(impl: PaymentRepositoryImpl): PaymentRepository

    @Binds
    abstract fun bindBannerRepository(impl: BannerRepositoryImpl): BannerRepository

    @Binds
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository

    companion object {
        @Provides
        @Singleton
        fun providePreferences(@ApplicationContext context: Context): SharedPreferences {
            return context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        }
    }
}

