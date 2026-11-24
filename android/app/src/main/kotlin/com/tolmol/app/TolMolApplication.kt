package com.tolmol.app

import android.app.Application
import com.tolmol.app.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class TolMolApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@TolMolApplication)
            modules(appModule)
        }
    }
}
