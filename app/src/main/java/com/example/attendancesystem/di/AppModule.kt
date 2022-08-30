package com.example.attendancesystem.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.attendancesystem.R
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideApplicationContext(
        @ApplicationContext
        context: Context
    ) = context

    @Singleton
    @Provides
    fun provideMainDispatcher() = Dispatchers.Main as CoroutineDispatcher

    @Singleton
    @Provides
    fun provideIODispatchers() = Dispatchers.IO

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(R.drawable.ic_baseline_image_24)
            .error(R.drawable.ic_baseline_error_outline_24)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )

}