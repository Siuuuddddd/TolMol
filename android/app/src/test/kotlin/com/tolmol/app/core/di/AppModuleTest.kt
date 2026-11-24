package com.tolmol.app.core.di

import android.content.Context
import org.junit.Test
import org.koin.test.verify.verify

class AppModuleTest {
    @Test fun verifyApplicationModule() {
        appModule.verify(extraTypes = listOf(Context::class))
    }
}
