package com.gsoft.opus.di

import com.gsoft.opus.data.repository.AuthRepositoryImpl
import com.gsoft.opus.data.repository.SettingsRepositoryImpl
import com.gsoft.opus.domain.repository.AuthRepository
import com.gsoft.opus.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
