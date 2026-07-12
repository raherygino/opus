package com.gsoft.opus.di

import com.gsoft.opus.data.signature.SignatureWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SignatureModule {

    @Provides
    @Singleton
    fun provideSignatureWebSocketClient(): SignatureWebSocketClient {
        return SignatureWebSocketClient()
    }
}
