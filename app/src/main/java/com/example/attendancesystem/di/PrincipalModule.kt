package com.example.attendancesystem.di

import com.example.attendancesystem.faculty.repository.DefaultFacultyRepository
import com.example.attendancesystem.faculty.repository.FacultyRepository
import com.example.attendancesystem.principal.repository.DefaultPrincipalRepository
import com.example.attendancesystem.principal.repository.PrincipalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrincipalModule {

    @Singleton
    @Provides
    fun providePrincipalRepository() = DefaultPrincipalRepository() as PrincipalRepository
}