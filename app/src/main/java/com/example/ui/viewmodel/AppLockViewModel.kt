package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppLockApplication
import com.example.domain.model.AuthResult
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.LockMethod
import com.example.domain.model.ProtectionHealth
import com.example.domain.model.VaultConfig
import com.example.security.BiometricCapability
import com.example.security.BiometricHelper
import com.example.security.CredentialHasher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AppsUiState {
    data object Loading : AppsUiState
    data class Ready(val apps: List<InstalledAppInfo>) : AppsUiState
}

class AppLockViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AppLockApplication
    private val repository = app.repository
    private val appsProvider = app.installedAppsProvider

    val config: StateFlow<VaultConfig> = repository.config

    private val _appsState = MutableStateFlow<AppsUiState>(AppsUiState.Loading)
    val appsState: StateFlow<AppsUiState> = _appsState.asStateFlow()

    private val _health = MutableStateFlow(repository.checkProtectionHealth(application))
    val health: StateFlow<ProtectionHealth> = _health.asStateFlow()

    private val _biometricCapability = MutableStateFlow(BiometricHelper.checkCapability(application))
    val biometricCapability: StateFlow<BiometricCapability> = _biometricCapability.asStateFlow()

    // In-app vault gate so opening App Lock itself requires authentication after onboarding
    private val _isSelfUnlocked = MutableStateFlow(!repository.config.value.isConfigured())
    val isSelfUnlocked: StateFlow<Boolean> = _isSelfUnlocked.asStateFlow()

    private var selfBackgroundedElapsedMs: Long? = null

    // Onboarding draft state
    private val _onboardingMethod = MutableStateFlow(LockMethod.PATTERN)
    val onboardingMethod: StateFlow<LockMethod> = _onboardingMethod.asStateFlow()

    private val _onboardingBiometric = MutableStateFlow(false)
    val onboardingBiometric: StateFlow<Boolean> = _onboardingBiometric.asStateFlow()

    private var pendingCredentialChars: CharArray? = null

    private val _onboardingLockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val onboardingLockedPackages: StateFlow<Set<String>> = _onboardingLockedPackages.asStateFlow()

    init {
        refreshHealth()
        loadInstalledApps()
    }

    fun refreshHealth() {
        val ctx = getApplication<Application>()
        _biometricCapability.value = BiometricHelper.checkCapability(ctx)
        _health.value = repository.checkProtectionHealth(ctx)
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            if (_appsState.value !is AppsUiState.Ready) {
                _appsState.value = AppsUiState.Loading
            }
            val locked = config.value.lockedPackages
            val list = appsProvider.loadLaunchableApps(locked)
            _appsState.value = AppsUiState.Ready(list)
        }
    }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? =
        appsProvider.loadAppIcon(packageName)

    fun setOnboardingMethod(method: LockMethod) {
        _onboardingMethod.value = method
    }

    fun setOnboardingBiometric(enabled: Boolean) {
        _onboardingBiometric.value = enabled
    }

    fun stageOnboardingCredential(rawChars: CharArray) {
        pendingCredentialChars?.fill('\u0000')
        pendingCredentialChars = rawChars
    }

    fun toggleOnboardingPackage(packageName: String) {
        val current = _onboardingLockedPackages.value
        _onboardingLockedPackages.value = if (packageName in current) {
            current - packageName
        } else {
            current + packageName
        }
    }

    fun finalizeOnboarding(onDone: () -> Unit) {
        val creds = pendingCredentialChars ?: return
        pendingCredentialChars = null
        viewModelScope.launch {
            repository.completeInitialSetup(
                method = _onboardingMethod.value,
                rawCredentialChars = creds,
                biometricEnabled = _onboardingBiometric.value && _biometricCapability.value.enrolled,
                lockedPackages = _onboardingLockedPackages.value
            )
            _isSelfUnlocked.value = true
            refreshHealth()
            loadInstalledApps()
            onDone()
        }
    }

    fun toggleInstalledAppLock(packageName: String, locked: Boolean) {
        viewModelScope.launch {
            repository.setAppLocked(packageName, locked)
            val current = _appsState.value
            if (current is AppsUiState.Ready) {
                val updatedList = current.apps.map {
                    if (it.packageName == packageName) it.copy(isLocked = locked) else it
                }
                _appsState.value = AppsUiState.Ready(updatedList)
            }
            refreshHealth()
        }
    }

    fun updateCredential(
        method: LockMethod,
        rawChars: CharArray,
        makePrimary: Boolean = true,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateCredential(method, rawChars, makePrimary)
            refreshHealth()
            onDone()
        }
    }

    fun verifySelfUnlock(
        method: LockMethod,
        rawChars: CharArray,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.verifyCredential(
                method = method,
                rawCredentialChars = rawChars,
                targetPackage = getApplication<Application>().packageName,
                targetAppName = "App Lock"
            )
            if (result is AuthResult.Success) {
                _isSelfUnlocked.value = true
                selfBackgroundedElapsedMs = null
            }
            onResult(result)
        }
    }

    fun onSelfBiometricSuccess() {
        viewModelScope.launch {
            repository.onBiometricUnlockSucceeded()
            _isSelfUnlocked.value = true
            selfBackgroundedElapsedMs = null
        }
    }

    fun getRemainingCooldownSeconds(): Int = repository.getRemainingCooldownSeconds()

    fun onMainActivityStopped() {
        if (config.value.isConfigured()) {
            selfBackgroundedElapsedMs = SystemClock.elapsedRealtime()
        }
    }

    fun onMainActivityStarted() {
        if (!config.value.isConfigured()) {
            _isSelfUnlocked.value = true
            return
        }
        val bgAt = selfBackgroundedElapsedMs ?: return
        val elapsed = SystemClock.elapsedRealtime() - bgAt
        val timeout = config.value.autoLockTimeout
        val shouldRelock = when (timeout) {
            AutoLockTimeout.IMMEDIATE -> true
            AutoLockTimeout.SCREEN_OFF -> false
            else -> elapsed >= timeout.durationMs
        }
        if (shouldRelock) {
            _isSelfUnlocked.value = false
        }
        selfBackgroundedElapsedMs = null
    }

    fun lockSelfNow() {
        if (config.value.isConfigured()) {
            _isSelfUnlocked.value = false
        }
    }

    fun setMasterProtection(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMasterProtectionEnabled(enabled)
            refreshHealth()
        }
    }

    fun setPrimaryMethod(method: LockMethod) {
        viewModelScope.launch {
            repository.setPrimaryMethod(method)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
            refreshHealth()
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            repository.setAutoLockTimeout(timeout)
        }
    }

    fun setLockOnScreenOff(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockOnScreenOff(enabled)
        }
    }

    fun setScreenCaptureProtection(enabled: Boolean) {
        viewModelScope.launch {
            repository.setScreenCaptureProtection(enabled)
        }
    }

    fun clearIntrusionEvents() {
        viewModelScope.launch {
            repository.clearIntrusionEvents()
        }
    }

    fun validatePattern(nodes: List<Int>): Boolean = CredentialHasher.isValidPattern(nodes)
    fun validatePin(pin: String): Boolean = CredentialHasher.isValidPin(pin)

    override fun onCleared() {
        pendingCredentialChars?.fill('\u0000')
        pendingCredentialChars = null
        super.onCleared()
    }
}
