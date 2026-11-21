package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.IntrusionEvent
import com.example.domain.model.LockMethod
import com.example.domain.model.ProtectionHealth
import com.example.domain.model.VaultConfig
import com.example.security.BiometricCapability
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen(
    config: VaultConfig,
    health: ProtectionHealth,
    onClearIntrusionEvents: () -> Unit,
    onOpenReliabilityHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Protection & Activity",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "System health diagnostics and local-only failed unlock attempts.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            HealthDiagnosticCard(
                title = "Accessibility Protection Service",
                subtitle = if (health.accessibilityEnabled) {
                    "Active — instant event-driven app detection"
                } else {
                    "Disabled — tap to enable in System Settings"
                },
                ok = health.accessibilityEnabled,
                icon = Icons.Rounded.Shield,
                testTag = "diag_accessibility_card",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }
            )
        }

        item {
            HealthDiagnosticCard(
                title = "Battery Optimization",
                subtitle = if (health.ignoringBatteryOptimizations) {
                    "Unrestricted — reduced risk of OEM service kills"
                } else {
                    "Optimized — tap to allow unrestricted background status"
                },
                ok = health.ignoringBatteryOptimizations,
                icon = Icons.Rounded.BatteryChargingFull,
                testTag = "diag_battery_card",
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            )
        }

        item {
            HealthDiagnosticCard(
                title = "Real-Device Reliability Guide",
                subtitle = "Android 13+ Restricted Settings, OEM rules & testing",
                ok = true,
                icon = Icons.Rounded.PhoneAndroid,
                testTag = "diag_device_guide_card",
                onClick = onOpenReliabilityHelp
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Failed Unlock Attempts (${config.intrusionEvents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                if (config.intrusionEvents.isNotEmpty()) {
                    TextButton(
                        onClick = onClearIntrusionEvents,
                        modifier = Modifier.testTag("clear_intrusions_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = DangerRed)
                    }
                }
            }
        }

        if (config.intrusionEvents.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MintSurface,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "No unauthorized attempts recorded",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Failed PIN or pattern entries are logged locally inside your encrypted vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        } else {
            items(config.intrusionEvents) { event ->
                IntrusionEventCard(event = event)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HealthDiagnosticCard(
    title: String,
    subtitle: String,
    ok: Boolean,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, if (ok) CardBorder else WarningAmber.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (ok) MintSurface else WarningSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (ok) EmeraldPrimary else WarningAmber
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(
                imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = if (ok) EmeraldPrimary else WarningAmber,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun IntrusionEventCard(event: IntrusionEvent) {
    val formatter = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())
    val timeLabel = formatter.format(Date(event.timestampMs))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DangerSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.targetAppName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Failed ${event.method.label} (attempt #${event.attemptCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun SettingsScreen(
    config: VaultConfig,
    biometricCapability: BiometricCapability,
    onBack: (() -> Unit)?,
    onChangePattern: () -> Unit,
    onChangePin: () -> Unit,
    onSwitchPrimaryMethod: (LockMethod) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onOpenAutoLockPicker: () -> Unit,
    onToggleScreenOffLock: (Boolean) -> Unit,
    onToggleScreenCaptureProtection: (Boolean) -> Unit,
    onOpenAppProtection: () -> Unit,
    onOpenPermissionsHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsNavRow(
            icon = Icons.Rounded.Apps,
            title = if (config.hasCredentialFor(LockMethod.PATTERN)) "Change Pattern" else "Set Up Pattern",
            subtitle = "Update your lock pattern",
            testTag = "settings_change_pattern",
            onClick = onChangePattern
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsNavRow(
            icon = Icons.Rounded.Dialpad,
            title = if (config.hasCredentialFor(LockMethod.PIN)) "Change PIN" else "Set Up PIN",
            subtitle = "Set a new PIN code",
            testTag = "settings_change_pin",
            onClick = onChangePin
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Primary lock method selector when both pattern and PIN are configured
        if (config.hasCredentialFor(LockMethod.PATTERN) && config.hasCredentialFor(LockMethod.PIN)) {
            SettingsNavRow(
                icon = Icons.Rounded.Shield,
                title = "Default Lock Method",
                subtitle = "Currently: ${config.primaryMethod.label} (tap to switch)",
                testTag = "settings_switch_primary_method",
                onClick = {
                    val next = if (config.primaryMethod == LockMethod.PATTERN) {
                        LockMethod.PIN
                    } else {
                        LockMethod.PATTERN
                    }
                    onSwitchPrimaryMethod(next)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        SettingsSwitchRow(
            icon = Icons.Rounded.Fingerprint,
            title = "Fingerprint Unlock",
            subtitle = when {
                biometricCapability.enrolled -> "Use fingerprint to unlock"
                biometricCapability.hardwareSupported -> "Enroll fingerprint in Device Settings"
                else -> "Unavailable on this device"
            },
            checked = config.biometricEnabled,
            enabled = biometricCapability.enrolled,
            testTag = "settings_biometric_switch",
            onCheckedChange = onToggleBiometric
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "General",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsNavRow(
            icon = Icons.Rounded.Timer,
            title = "Auto Lock",
            subtitle = config.autoLockTimeout.summary,
            testTag = "settings_auto_lock",
            onClick = onOpenAutoLockPicker
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsSwitchRow(
            icon = Icons.Rounded.LockClock,
            title = "Relock on Screen Off",
            subtitle = "Immediately relock all apps when screen turns off",
            checked = config.lockOnScreenOff,
            enabled = true,
            testTag = "settings_screen_off_switch",
            onCheckedChange = onToggleScreenOffLock
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsSwitchRow(
            icon = Icons.Rounded.Screenshot,
            title = "Screen Capture Protection",
            subtitle = "Block screenshots & recording on lock screens",
            checked = config.screenCaptureProtection,
            enabled = true,
            testTag = "settings_flag_secure_switch",
            onCheckedChange = onToggleScreenCaptureProtection
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsNavRow(
            icon = Icons.Rounded.VerifiedUser,
            title = "App Protection",
            subtitle = "Manage protected apps (${config.lockedPackages.size} locked)",
            testTag = "settings_app_protection",
            onClick = onOpenAppProtection
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsNavRow(
            icon = Icons.Rounded.PhoneAndroid,
            title = "Permissions & Reliability",
            subtitle = "Accessibility, battery optimization & OEM guide",
            testTag = "settings_permissions_guide",
            onClick = onOpenPermissionsHelp
        )
        Spacer(modifier = Modifier.height(10.dp))

        SettingsNavRow(
            icon = Icons.Rounded.Info,
            title = "About App Lock",
            subtitle = "Version 1.0.0 • Offline Keystore Encryption",
            testTag = "settings_about",
            onClick = onOpenAbout
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Bottom Privacy Priority Card matching reference Settings screen
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MintSurface,
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Your privacy is our priority",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "No data is collected or shared.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = EmeraldPrimary
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
