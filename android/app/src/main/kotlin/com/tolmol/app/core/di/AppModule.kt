package com.tolmol.app.core.di

import com.tolmol.app.core.datastore.SessionStore
import com.tolmol.app.core.network.networkModule
import com.tolmol.app.core.websocket.LiveSocket
import com.tolmol.app.feature.auth.data.AuthApi
import com.tolmol.app.feature.auth.data.AuthRepositoryImpl
import com.tolmol.app.feature.auth.domain.AuthRepository
import com.tolmol.app.feature.auth.domain.AuthUseCase
import com.tolmol.app.feature.auth.presentation.AuthViewModel
import com.tolmol.app.feature.bidding.data.BiddingApi
import com.tolmol.app.feature.bidding.data.BiddingRepositoryImpl
import com.tolmol.app.feature.bidding.domain.BiddingRepository
import com.tolmol.app.feature.bidding.domain.BiddingUseCase
import com.tolmol.app.feature.bidding.presentation.BiddingViewModel
import com.tolmol.app.feature.bidding.presentation.MyBidsViewModel
import com.tolmol.app.feature.chat.data.ChatApi
import com.tolmol.app.feature.chat.data.ChatRepositoryImpl
import com.tolmol.app.feature.chat.domain.ChatRepository
import com.tolmol.app.feature.chat.domain.ChatUseCase
import com.tolmol.app.feature.chat.presentation.ChatViewModel
import com.tolmol.app.feature.chat.presentation.ConversationsViewModel
import com.tolmol.app.feature.chat.presentation.OpenChatViewModel
import com.tolmol.app.feature.deal.data.DealApi
import com.tolmol.app.feature.deal.data.DealRepositoryImpl
import com.tolmol.app.feature.deal.domain.DealRepository
import com.tolmol.app.feature.deal.domain.DealUseCase
import com.tolmol.app.feature.deal.presentation.DealViewModel
import com.tolmol.app.feature.deal.presentation.DealsViewModel
import com.tolmol.app.feature.discovery.data.DiscoveryApi
import com.tolmol.app.feature.discovery.data.DiscoveryRepositoryImpl
import com.tolmol.app.feature.discovery.domain.DiscoveryRepository
import com.tolmol.app.feature.discovery.domain.DiscoveryUseCase
import com.tolmol.app.feature.discovery.presentation.DiscoveryViewModel
import com.tolmol.app.feature.listing.data.ListingApi
import com.tolmol.app.feature.listing.data.ListingRepositoryImpl
import com.tolmol.app.feature.listing.domain.ListingRepository
import com.tolmol.app.feature.listing.domain.ListingUseCase
import com.tolmol.app.feature.listing.presentation.ListingViewModel
import com.tolmol.app.feature.listing.presentation.OffersViewModel
import com.tolmol.app.feature.listing.presentation.PublishViewModel
import com.tolmol.app.feature.location.data.LocationApi
import com.tolmol.app.feature.location.data.LocationRepositoryImpl
import com.tolmol.app.feature.location.domain.LocationRepository
import com.tolmol.app.feature.location.domain.LocationUseCase
import com.tolmol.app.feature.location.presentation.LocationViewModel
import com.tolmol.app.feature.notification.data.NotificationApi
import com.tolmol.app.feature.notification.data.NotificationRepositoryImpl
import com.tolmol.app.feature.notification.domain.NotificationRepository
import com.tolmol.app.feature.notification.domain.NotificationUseCase
import com.tolmol.app.feature.notification.presentation.NotificationViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit

val appModule = module {
    includes(networkModule)
    singleOf(::SessionStore)
    singleOf(::LiveSocket)

    single { get<Retrofit>().create(AuthApi::class.java) }
    singleOf(::AuthRepositoryImpl) bind AuthRepository::class
    factoryOf(::AuthUseCase)
    viewModelOf(::AuthViewModel)

    single { get<Retrofit>().create(LocationApi::class.java) }
    singleOf(::LocationRepositoryImpl) bind LocationRepository::class
    factoryOf(::LocationUseCase)
    viewModelOf(::LocationViewModel)

    single { get<Retrofit>().create(DiscoveryApi::class.java) }
    singleOf(::DiscoveryRepositoryImpl) bind DiscoveryRepository::class
    factoryOf(::DiscoveryUseCase)
    viewModelOf(::DiscoveryViewModel)

    single { get<Retrofit>().create(BiddingApi::class.java) }
    singleOf(::BiddingRepositoryImpl) bind BiddingRepository::class
    factoryOf(::BiddingUseCase)
    viewModelOf(::BiddingViewModel)
    viewModelOf(::MyBidsViewModel)

    single { get<Retrofit>().create(ListingApi::class.java) }
    singleOf(::ListingRepositoryImpl) bind ListingRepository::class
    factoryOf(::ListingUseCase)
    viewModelOf(::ListingViewModel)
    viewModelOf(::PublishViewModel)
    viewModelOf(::OffersViewModel)

    single { get<Retrofit>().create(ChatApi::class.java) }
    singleOf(::ChatRepositoryImpl) bind ChatRepository::class
    factoryOf(::ChatUseCase)
    viewModelOf(::ChatViewModel)
    viewModelOf(::ConversationsViewModel)
    viewModelOf(::OpenChatViewModel)

    single { get<Retrofit>().create(NotificationApi::class.java) }
    singleOf(::NotificationRepositoryImpl) bind NotificationRepository::class
    factoryOf(::NotificationUseCase)
    viewModelOf(::NotificationViewModel)

    single { get<Retrofit>().create(DealApi::class.java) }
    singleOf(::DealRepositoryImpl) bind DealRepository::class
    factoryOf(::DealUseCase)
    viewModelOf(::DealViewModel)
    viewModelOf(::DealsViewModel)
}
