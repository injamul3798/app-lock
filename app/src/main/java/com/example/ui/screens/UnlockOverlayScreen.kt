package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.AuthResult
import com.example.domain.model.LockMethod
import com.example.domain.model.VaultConfig
import com.example.security.CredentialHasher
import com.example.ui.components.PatternLockPad
import com.example.ui.components.PinLockPad
import com.example.ui.components.ShieldLockHero
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.ForestOverlayBottom
import com.example.ui.theme.ForestOverlayCenter
import com.example.ui.theme.ForestOverlayTop
import kotlinx.coroutines.delay

@Composable
fun UnlockOverlayScreen(
    config: VaultConfig,
    targetAppName: String,
    biometricAvailable: Boolean,
    initialCooldownSeconds: Int,
    onVerifyCredential: (LockMethod, CharArray, (AuthResult) -> Unit) -> Unit,
    onRequestBiometric: () -> Unit
) {
    var activeMethod by remember(config.primaryMethod) {
        mutableStateOf(
            if (config.hasCredentialFor(config.primaryMethod)) config.primaryMethod
            else if (config.hasCredentialFor(LockMethod.PATTERN)) LockMethod.PATTERN
            else LockMethod.PIN
        )
    }

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cooldownSeconds by remember(initialCooldownSeconds) { mutableIntStateOf(initialCooldownSeconds) }
    var hasAutoPromptedBiometric by remember { mutableStateOf(false) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1_000L)
            cooldownSeconds -= 1
            if (cooldownSeconds == 0) {
                errorMessage = null
            }
        }
    }

    LaunchedEffect(config.biometricEnabled, biometricAvailable, cooldownSeconds) {
        if (config.biometricEnabled && biometricAvailable && cooldownSeconds == 0 && !hasAutoPromptedBiometric) {
            hasAutoPromptedBiometric = true
            onRequestBiometric()
        }
    }

    val isLockedOut = cooldownSeconds > 0
    val alternateMethod = if (activeMethod == LockMethod.PATTERN && config.hasCredentialFor(LockMethod.PIN)) {
        LockMethod.PIN
    } else if (activeMethod == LockMethod.PIN && config.hasCredentialFor(LockMethod.PATTERN)) {
        LockMethod.PATTERN
    } else {
        null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(ForestOverlayCenter, ForestOverlayTop, ForestOverlayBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ShieldLockHero(size = 96.dp, darkBackdrop = true)
                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = targetAppName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Unlock to Continue",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        isLockedOut -> "Too many attempts. Try again in ${cooldownSeconds}s"
                        activeMethod == LockMethod.PATTERN -> "Draw your pattern"
                        else -> "Enter your PIN"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isLockedOut) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )

                AnimatedVisibility(visible = errorMessage != null && !isLockedOut) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (activeMethod == LockMethod.PATTERN) {
                PatternLockPad(
                    darkMode = true,
                    enabled = !isLockedOut,
                    isError = errorMessage != null || isLockedOut,
                    onPatternComplete = { nodes ->
                        if (nodes.size < 4) {
                            errorMessage = "Connect at least 4 dots"
                            return@PatternLockPad
                        }
                        onVerifyCredential(
                            LockMethod.PATTERN,
                            CredentialHasher.formatPattern(nodes)
                        ) { result ->
                            when (result) {
                                is AuthResult.Success -> errorMessage = null
                                is AuthResult.Failed -> {
                                    if (result.cooldownSeconds > 0) {
                                        cooldownSeconds = result.cooldownSeconds
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect pattern (${result.attemptCount})"
                                    }
                                }
                                is AuthResult.LockedOut -> {
                                    cooldownSeconds = result.remainingSeconds
                                }
                            }
                        }
                    }
                )
            } else {
                PinLockPad(
                    pin = pinInput,
                    darkMode = true,
                    enabled = !isLockedOut,
                    isError = errorMessage != null || isLockedOut,
                    showBiometricKey = true,
                    onDigitPress = { d ->
                        errorMessage = null
                        if (pinInput.length < 6) pinInput += d
                    },
                    onBackspace = {
                        errorMessage = null
                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                    },
                    onSubmit = {
                        val chars = pinInput.toCharArray()
                        pinInput = ""
                        onVerifyCredential(LockMethod.PIN, chars) { result ->
                            when (result) {
                                is AuthResult.Success -> errorMessage = null
                                is AuthResult.Failed -> {
                                    if (result.cooldownSeconds > 0) {
                                        cooldownSeconds = result.cooldownSeconds
                                        errorMessage = null
                                    } else {
                                        errorMessage = "Incorrect PIN (${result.attemptCount})"
                                    }
                                }
                                is AuthResult.LockedOut -> {
                                    cooldownSeconds = result.remainingSeconds
                                }
                            }
                        }
                    }
                )
            }

            // Bottom actions: Use Fingerprint & Switch Method (if both configured)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.biometricEnabled && biometricAvailable && !isLockedOut) {
                    TextButton(
                        onClick = onRequestBiometric,
                        modifier = Modifier.testTag("use_fingerprint_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Use Fingerprint",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White
                        )
                    }
                }

                if (alternateMethod != null) {
                    if (config.biometricEnabled && biometricAvailable && !isLockedOut) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    TextButton(
                        onClick = {
                            pinInput = ""
                            errorMessage = null
                            activeMethod = alternateMethod
                        },
                        modifier = Modifier.testTag("switch_unlock_method_button")
                    ) {
                        Text(
                            text = if (alternateMethod == LockMethod.PIN) "Use PIN" else "Use Pattern",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
