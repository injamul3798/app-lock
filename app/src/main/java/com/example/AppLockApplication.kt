package com.example

import android.app.Application
import com.example.data.AppLockRepository
import com.example.data.InstalledAppsProvider
import com.example.engine.AppLockEngine
import com.example.security.AndroidKeyStoreKeyProvider
import com.example.security.EncryptedVaultStore
import java.io.File

class AppLockApplication : Application() {

    lateinit var installedAppsProvider: InstalledAppsProvider
        private set

    lateinit var appLockEngine: AppLockEngine
        private set

    lateinit var repository: AppLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        installedAppsProvider = InstalledAppsProvider(this)
        appLockEngine = AppLockEngine(ownPackageName = packageName)
        val vaultStore = EncryptedVaultStore(
            vaultFile = File(filesDir, "applock_vault.enc"),
            keyProvider = AndroidKeyStoreKeyProvider()
        )
        repository = AppLockRepository(
            vaultStore = vaultStore,
            engine = appLockEngine
        )
    }
}
