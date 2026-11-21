package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import com.example.AppLockApplication
import com.example.domain.model.LockMethod
import com.example.security.BiometricHelper
import com.example.ui.screens.UnlockOverlayScreen
import com.example.ui.theme.AppLockTheme
import kotlinx.coroutines.launch

/**
 * Isolated lock overlay Activity protecting locked applications.
 * Secured with FLAG_SECURE, distinct taskAffinity, excludeFromRecents=true,
 * and Home redirection on Back press to prevent bypass.
 */
class LockScreenActivity : FragmentActivity() {

    private var targetPackage: String = ""
    private var targetAppName: String = "Protected App"
    private var unlockedSuccessfully = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = applicationContext as AppLockApplication
        val configSnapshot = app.repository.config.value
        if (configSnapshot.screenCaptureProtection) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        extractTarget(intent)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToHome()
                }
            }
        )

        val biometricCapability = BiometricHelper.checkCapability(this)

        setContent {
            AppLockTheme {
                val config by app.repository.config.collectAsState()
                val scope = rememberCoroutineScope()

                UnlockOverlayScreen(
                    config = config,
                    targetAppName = targetAppName,
                    biometricAvailable = biometricCapability.enrolled,
                    initialCooldownSeconds = app.repository.getRemainingCooldownSeconds(),
                    onVerifyCredential = { method: LockMethod, chars: CharArray, callback ->
                        scope.launch {
                            val result = app.repository.verifyCredential(
                                method = method,
                                rawCredentialChars = chars,
                                targetPackage = targetPackage,
                                targetAppName = targetAppName
                            )
                            if (result is com.example.domain.model.AuthResult.Success) {
                                unlockedSuccessfully = true
                                app.appLockEngine.onUnlockSucceeded(targetPackage)
                                finish()
                                @Suppress("DEPRECATION")
                                overridePendingTransition(0, 0)
                            }
                            callback(result)
                        }
                    },
                    onRequestBiometric = {
                        if (app.repository.getRemainingCooldownSeconds() == 0) {
                            BiometricHelper.authenticate(
                                activity = this@LockScreenActivity,
                                title = "Unlock $targetAppName",
                                subtitle = "Verify your fingerprint to continue",
                                negativeButtonText = "Use ${config.primaryMethod.label}",
                                onSuccess = {
                                    scope.launch {
                                        app.repository.onBiometricUnlockSucceeded()
                                        unlockedSuccessfully = true
                                        app.appLockEngine.onUnlockSucceeded(targetPackage)
                                        finish()
                                        @Suppress("DEPRECATION")
                                        overridePendingTransition(0, 0)
                                    }
                                },
                                onFallback = { /* User chose pattern/PIN */ },
                                onFailureMessage = { /* Handled by system biometric sheet */ }
                            )
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTarget(intent)
    }

    override fun onStop() {
        super.onStop()
        if (!unlockedSuccessfully && !isChangingConfigurations) {
            val app = applicationContext as? AppLockApplication
            app?.appLockEngine?.onUnlockCancelled(targetPackage)
            finish()
        }
    }

    private fun extractTarget(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        targetPackage = pkg
        val app = applicationContext as? AppLockApplication
        targetAppName = if (pkg.isNotBlank() && app != null) {
            app.installedAppsProvider.getAppLabel(pkg)
        } else {
            "Protected App"
        }
    }

    private fun navigateToHome() {
        val app = applicationContext as? AppLockApplication
        app?.appLockEngine?.onUnlockCancelled(targetPackage)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val EXTRA_TARGET_PACKAGE = "extra_target_package"

        fun createIntent(context: Context, targetPackage: String): Intent =
            Intent(context, LockScreenActivity::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
            }
    }
}
