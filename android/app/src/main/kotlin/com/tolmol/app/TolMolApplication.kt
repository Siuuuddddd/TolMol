package com.tolmol.app

import android.app.Application
import com.tolmol.app.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TolMolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val koinApp=startKoin {
            androidContext(this@TolMolApplication)
            modules(appModule)
        }
        coil3.SingletonImageLoader.setSafe {
            coil3.ImageLoader.Builder(this)
                .components { add(coil3.network.okhttp.OkHttpNetworkFetcherFactory(callFactory={ koinApp.koin.get<okhttp3.OkHttpClient>() })) }
                .build()
        }
    }
}
