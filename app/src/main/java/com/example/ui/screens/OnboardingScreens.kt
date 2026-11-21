package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.model.LockMethod
import com.example.domain.model.ProtectionHealth
import com.example.security.BiometricCapability
import com.example.security.CredentialHasher
import com.example.ui.components.CelebrationCheckBadge
import com.example.ui.components.PatternLockPad
import com.example.ui.components.PinLockPad
import com.example.ui.components.ShieldLockHero
import com.example.ui.components.SoftMintBackdrop
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSurface

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SoftMintBackdrop(showBottomWave = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShieldLockHero(size = 152.dp)
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "App Lock",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep your apps private\nand secure",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WelcomeFeatureBadge(
                        icon = Icons.Rounded.VerifiedUser,
                        label = "Lock\nApps"
                    )
                    WelcomeFeatureBadge(
                        icon = Icons.Rounded.Fingerprint,
                        label = "Use\nFingerprint"
                    )
                    WelcomeFeatureBadge(
                        icon = Icons.Rounded.Bolt,
                        label = "Lightweight"
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "100% offline • No accounts • Zero tracking",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGetStarted,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("get_started_button")
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureBadge(
    icon: ImageVector,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(92.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MintSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChooseLockMethodScreen(
    selectedMethod: LockMethod,
    biometricEnabled: Boolean,
    biometricCapability: BiometricCapability,
    onSelectMethod: (LockMethod) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SoftMintBackdrop(showBottomWave = false)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("choose_method_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose Lock Method",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Select how you want to lock\nyour apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            LockMethodCard(
                title = "Pattern Lock",
                subtitle = "Draw a pattern",
                icon = Icons.Rounded.Apps,
                selected = selectedMethod == LockMethod.PATTERN,
                testTag = "method_pattern_card",
                onClick = { onSelectMethod(LockMethod.PATTERN) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            LockMethodCard(
                title = "PIN",
                subtitle = "Enter a PIN code",
                icon = Icons.Rounded.Dialpad,
                selected = selectedMethod == LockMethod.PIN,
                testTag = "method_pin_card",
                onClick = { onSelectMethod(LockMethod.PIN) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Fingerprint option pairs with Pattern or PIN fallback per Android security best practice
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                border = BorderStroke(
                    width = if (biometricEnabled) 1.5.dp else 1.dp,
                    color = if (biometricEnabled) EmeraldPrimary else CardBorder
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("method_fingerprint_card")
                    .clickable(enabled = biometricCapability.enrolled) {
                        onToggleBiometric(!biometricEnabled)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (biometricEnabled) MintContainer else Color(0xFFF3F6F4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = if (biometricCapability.enrolled) EmeraldPrimary else TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Fingerprint",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                biometricCapability.enrolled -> "Also unlock with fingerprint"
                                biometricCapability.hardwareSupported -> "Enroll a fingerprint in Device Settings first"
                                else -> "Biometric hardware unavailable on device"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onToggleBiometric,
                        enabled = biometricCapability.enrolled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinue,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("choose_method_continue_button")
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LockMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) EmeraldPrimary else CardBorder
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) MintContainer else Color(0xFFF3F6F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) EmeraldPrimary else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Selected",
                tint = EmeraldPrimary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun CredentialSetupScreen(
    method: LockMethod,
    isChangeMode: Boolean,
    onBack: () -> Unit,
    onCredentialConfirmed: (CharArray) -> Unit
) {
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var firstPin by remember { mutableStateOf<String?>(null) }
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isConfirmStep = if (method == LockMethod.PATTERN) firstPattern != null else firstPin != null

    val title = when (method) {
        LockMethod.PATTERN -> when {
            isConfirmStep -> "Confirm Pattern"
            isChangeMode -> "New Pattern"
            else -> "Draw Pattern"
        }
        LockMethod.PIN -> when {
            isConfirmStep -> "Confirm PIN"
            isChangeMode -> "New PIN"
            else -> "Create PIN"
        }
    }

    val subtitle = when (method) {
        LockMethod.PATTERN ->
            if (isConfirmStep) "Draw the same pattern again to confirm" else "Connect at least 4 dots"
        LockMethod.PIN ->
            if (isConfirmStep) "Re-enter your PIN code to confirm" else "Enter a 4 to 6 digit PIN code"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SoftMintBackdrop(showBottomWave = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("credential_setup_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DangerRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (method == LockMethod.PATTERN) {
                PatternLockPad(
                    darkMode = false,
                    isError = errorMessage != null,
                    onPatternComplete = { nodes ->
                        if (!CredentialHasher.isValidPattern(nodes)) {
                            errorMessage = "Connect at least 4 dots. Try again."
                            return@PatternLockPad
                        }
                        val initial = firstPattern
                        if (initial == null) {
                            firstPattern = nodes
                            errorMessage = null
                        } else if (initial == nodes) {
                            errorMessage = null
                            onCredentialConfirmed(CredentialHasher.formatPattern(nodes))
                        } else {
                            errorMessage = "Patterns do not match. Draw again."
                        }
                    }
                )
            } else {
                PinLockPad(
                    pin = pinInput,
                    darkMode = false,
                    isError = errorMessage != null,
                    showBiometricKey = false,
                    onDigitPress = { digit ->
                        errorMessage = null
                        if (pinInput.length < 6) pinInput += digit
                    },
                    onBackspace = {
                        errorMessage = null
                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                    },
                    onSubmit = {
                        if (!CredentialHasher.isValidPin(pinInput)) {
                            errorMessage = "Enter a 4 to 6 digit PIN."
                            return@PinLockPad
                        }
                        val initial = firstPin
                        if (initial == null) {
                            firstPin = pinInput
                            pinInput = ""
                            errorMessage = null
                        } else if (initial == pinInput) {
                            val chars = pinInput.toCharArray()
                            pinInput = ""
                            firstPin = null
                            errorMessage = null
                            onCredentialConfirmed(chars)
                        } else {
                            pinInput = ""
                            errorMessage = "PINs do not match. Try again."
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    if (isConfirmStep) {
                        firstPattern = null
                        firstPin = null
                        pinInput = ""
                        errorMessage = null
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier.testTag("credential_cancel_button")
            ) {
                Text(
                    text = if (isConfirmStep) "Redraw / Start Over" else "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun AllSetScreen(
    lockedCount: Int,
    health: ProtectionHealth,
    onRefreshHealth: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var showRestrictedTip by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SoftMintBackdrop(showBottomWave = true)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CelebrationCheckBadge()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "All Set!",
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (lockedCount > 0) {
                        "$lockedCount selected app${if (lockedCount == 1) " is" else "s are"} configured for App Lock."
                    } else {
                        "Your vault is ready. You can select apps to lock anytime."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real permission readiness check so we never pretend protection is active without Accessibility
            if (!health.accessibilityEnabled) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WarningSurface),
                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "One Step Left: Enable Protection Service",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Android requires Accessibility permission to detect when a protected app opens. App Lock never reads screen content and runs 100% offline.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                showRestrictedTip = true
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("all_set_enable_accessibility_button")
                        ) {
                            Text("Open Accessibility Settings")
                        }
                        if (showRestrictedTip) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                        )
                                    }
                                },
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Android 13+ Restricted? Open App Info")
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MintSurface,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Protection Service is active and monitoring protected apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onRefreshHealth()
                    onFinish()
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("all_set_great_button")
            ) {
                Text(
                    text = "Great!",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
