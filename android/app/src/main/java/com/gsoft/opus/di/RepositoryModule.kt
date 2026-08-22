package com.gsoft.opus.di

import com.gsoft.opus.data.repository.AuthRepositoryImpl
import com.gsoft.opus.data.repository.ComportementRepositoryImpl
import com.gsoft.opus.data.repository.CorrespondanceRepositoryImpl
import com.gsoft.opus.data.repository.DeviceTokenRepositoryImpl
import com.gsoft.opus.data.repository.MouvementRepositoryImpl
import com.gsoft.opus.data.repository.NotificationRepositoryImpl
import com.gsoft.opus.data.repository.PersonnelRepositoryImpl
import com.gsoft.opus.data.repository.QrAuthRepositoryImpl
import com.gsoft.opus.data.repository.SettingsRepositoryImpl
import com.gsoft.opus.domain.repository.AuthRepository
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.repository.DeviceTokenRepository
import com.gsoft.opus.domain.repository.MouvementRepository
import com.gsoft.opus.domain.repository.NotificationRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.QrAuthRepository
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

    @Binds
    @Singleton
    abstract fun bindDeviceTokenRepository(impl: DeviceTokenRepositoryImpl): DeviceTokenRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindPersonnelRepository(impl: PersonnelRepositoryImpl): PersonnelRepository

    @Binds
    @Singleton
    abstract fun bindMouvementRepository(impl: MouvementRepositoryImpl): MouvementRepository

    @Binds
    @Singleton
    abstract fun bindComportementRepository(impl: ComportementRepositoryImpl): ComportementRepository

    @Binds
    @Singleton
    abstract fun bindCorrespondanceRepository(impl: CorrespondanceRepositoryImpl): CorrespondanceRepository

    @Binds
    @Singleton
    abstract fun bindQrAuthRepository(impl: QrAuthRepositoryImpl): QrAuthRepository
}
