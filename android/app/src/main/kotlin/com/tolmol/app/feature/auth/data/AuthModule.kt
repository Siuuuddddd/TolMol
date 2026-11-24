package com.tolmol.app.feature.auth.data

import com.tolmol.app.core.datastore.SessionStore
import com.tolmol.app.feature.auth.domain.AuthUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides fun auth(retrofit: Retrofit, session: SessionStore): AuthUseCase =
        AuthUseCase(AuthRepositoryImpl(retrofit.create(AuthApi::class.java), session))
}
