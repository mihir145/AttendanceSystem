package com.example.attendancesystem.di

import com.example.attendancesystem.authorization.repository.AuthRepository
import com.example.attendancesystem.authorization.repository.DefaultAuthRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthorizationModule {

    @Singleton
    @Provides
    fun provideAuthRepository() = DefaultAuthRepository() as AuthRepository
}