package com.gsoft.opus.di

import com.gsoft.opus.data.repository.ArmementRepositoryImpl
import com.gsoft.opus.data.repository.ArmeRepositoryImpl
import com.gsoft.opus.data.repository.AuthRepositoryImpl
import com.gsoft.opus.data.repository.ComportementRepositoryImpl
import com.gsoft.opus.data.repository.CorrespondanceRepositoryImpl
import com.gsoft.opus.data.repository.DeclarationPerteRepositoryImpl
import com.gsoft.opus.data.repository.DeviceTokenRepositoryImpl
import com.gsoft.opus.data.repository.MouvementRepositoryImpl
import com.gsoft.opus.data.repository.NotificationRepositoryImpl
import com.gsoft.opus.data.repository.PassationRepositoryImpl
import com.gsoft.opus.data.repository.PersonnelRepositoryImpl
import com.gsoft.opus.data.repository.QrAuthRepositoryImpl
import com.gsoft.opus.data.repository.SettingsRepositoryImpl
import com.gsoft.opus.data.repository.MaterielRepositoryImpl
import com.gsoft.opus.data.repository.MaterielRoulantRepositoryImpl
import com.gsoft.opus.data.repository.MainCouranteRepositoryImpl
import com.gsoft.opus.data.repository.MainCouranteCategorieRepositoryImpl
import com.gsoft.opus.data.repository.PlainteRepositoryImpl
import com.gsoft.opus.data.repository.ConvocationRepositoryImpl
import com.gsoft.opus.data.repository.GardeAVueRepositoryImpl
import com.gsoft.opus.data.repository.RequisitionRepositoryImpl
import com.gsoft.opus.data.repository.PersonneRechercheeRepositoryImpl
import com.gsoft.opus.data.repository.ObjetSaisiRepositoryImpl
import com.gsoft.opus.data.repository.ObjetTrouveRepositoryImpl
import com.gsoft.opus.data.repository.PerquisitionRepositoryImpl
import com.gsoft.opus.data.repository.RenseignementPjRepositoryImpl
import com.gsoft.opus.data.repository.MandatRepositoryImpl
import com.gsoft.opus.domain.repository.ArmementRepository
import com.gsoft.opus.domain.repository.ArmeRepository
import com.gsoft.opus.domain.repository.AuthRepository
import com.gsoft.opus.domain.repository.ComportementRepository
import com.gsoft.opus.domain.repository.CorrespondanceRepository
import com.gsoft.opus.domain.repository.DeclarationPerteRepository
import com.gsoft.opus.domain.repository.DeviceTokenRepository
import com.gsoft.opus.domain.repository.MouvementRepository
import com.gsoft.opus.domain.repository.NotificationRepository
import com.gsoft.opus.domain.repository.PassationRepository
import com.gsoft.opus.domain.repository.PersonnelRepository
import com.gsoft.opus.domain.repository.QrAuthRepository
import com.gsoft.opus.domain.repository.SettingsRepository
import com.gsoft.opus.domain.repository.MaterielRepository
import com.gsoft.opus.domain.repository.MaterielRoulantRepository
import com.gsoft.opus.domain.repository.MainCouranteRepository
import com.gsoft.opus.domain.repository.MainCouranteCategorieRepository
import com.gsoft.opus.domain.repository.PlainteRepository
import com.gsoft.opus.domain.repository.ConvocationRepository
import com.gsoft.opus.domain.repository.GardeAVueRepository
import com.gsoft.opus.domain.repository.RequisitionRepository
import com.gsoft.opus.domain.repository.PersonneRechercheeRepository
import com.gsoft.opus.domain.repository.ObjetSaisiRepository
import com.gsoft.opus.domain.repository.ObjetTrouveRepository
import com.gsoft.opus.domain.repository.PerquisitionRepository
import com.gsoft.opus.domain.repository.RenseignementPjRepository
import com.gsoft.opus.domain.repository.MandatRepository
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
    abstract fun bindDeclarationPerteRepository(impl: DeclarationPerteRepositoryImpl): DeclarationPerteRepository

    @Binds
    @Singleton
    abstract fun bindPassationRepository(impl: PassationRepositoryImpl): PassationRepository

    @Binds
    @Singleton
    abstract fun bindArmementRepository(impl: ArmementRepositoryImpl): ArmementRepository

    @Binds
    @Singleton
    abstract fun bindArmeRepository(impl: ArmeRepositoryImpl): ArmeRepository

    @Binds
    @Singleton
    abstract fun bindQrAuthRepository(impl: QrAuthRepositoryImpl): QrAuthRepository

    @Binds
    @Singleton
    abstract fun bindMaterielRepository(impl: MaterielRepositoryImpl): MaterielRepository

    @Binds
    @Singleton
    abstract fun bindMaterielRoulantRepository(impl: MaterielRoulantRepositoryImpl): MaterielRoulantRepository

    @Binds
    @Singleton
    abstract fun bindMainCouranteRepository(impl: MainCouranteRepositoryImpl): MainCouranteRepository

    @Binds
    @Singleton
    abstract fun bindMainCouranteCategorieRepository(impl: MainCouranteCategorieRepositoryImpl): MainCouranteCategorieRepository

    @Binds
    @Singleton
    abstract fun bindPlainteRepository(impl: PlainteRepositoryImpl): PlainteRepository

    @Binds
    @Singleton
    abstract fun bindConvocationRepository(impl: ConvocationRepositoryImpl): ConvocationRepository

    @Binds
    @Singleton
    abstract fun bindGardeAVueRepository(impl: GardeAVueRepositoryImpl): GardeAVueRepository

    @Binds
    @Singleton
    abstract fun bindRequisitionRepository(impl: RequisitionRepositoryImpl): RequisitionRepository

    @Binds
    @Singleton
    abstract fun bindPersonneRechercheeRepository(impl: PersonneRechercheeRepositoryImpl): PersonneRechercheeRepository

    @Binds
    @Singleton
    abstract fun bindObjetSaisiRepository(impl: ObjetSaisiRepositoryImpl): ObjetSaisiRepository

    @Binds
    @Singleton
    abstract fun bindObjetTrouveRepository(impl: ObjetTrouveRepositoryImpl): ObjetTrouveRepository

    @Binds
    @Singleton
    abstract fun bindPerquisitionRepository(impl: PerquisitionRepositoryImpl): PerquisitionRepository

    @Binds
    @Singleton
    abstract fun bindRenseignementPjRepository(impl: RenseignementPjRepositoryImpl): RenseignementPjRepository

    @Binds
    @Singleton
    abstract fun bindMandatRepository(impl: MandatRepositoryImpl): MandatRepository
}
