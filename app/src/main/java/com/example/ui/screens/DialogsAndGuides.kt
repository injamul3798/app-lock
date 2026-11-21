package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AutoLockTimeout
import com.example.domain.model.ProtectionHealth
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AutoLockTimeoutDialog(
    current: AutoLockTimeout,
    onSelect: (AutoLockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Auto Lock Timeout", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AutoLockTimeout.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = current == option,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = option.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EmeraldPrimary)
            }
        }
    )
}

@Composable
fun PrivacyInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Privacy & Security Architecture", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                BulletSection(
                    title = "100% Offline Design",
                    body = "App Lock declares zero internet or network permissions. No telemetry, analytics, ads, or cloud accounts exist."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "Salted Credential Hashing",
                    body = "Your pattern or PIN is never stored in plaintext. It is hashed with PBKDF2-HMAC-SHA256 (120,000 iterations) and a unique 16-byte random salt."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "Android Keystore Vault",
                    body = "Security configuration and locked app lists are encrypted locally with AES-256-GCM keys inside Android Keystore and excluded from cloud backups."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "Minimal Accessibility Scope",
                    body = "App Lock only observes foreground window package switches (canRetrieveWindowContent=false) and never reads screen text or keystrokes."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun HelpAndReliabilityDialog(
    health: ProtectionHealth,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Reliability & Device Setup", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                BulletSection(
                    title = "1. Protection Service (Required)",
                    body = if (health.accessibilityEnabled) {
                        "Status: Enabled. Android delivers foreground window events directly with zero continuous polling."
                    } else {
                        "Status: Disabled. Open Accessibility Settings and enable 'App Lock Protection Service'."
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "2. Android 13+ Restricted Settings",
                    body = "If you installed via APK outside an app store and the Accessibility toggle is grayed out, open App Info -> tap the 3-dot menu (top right) -> 'Allow restricted settings'."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "3. Battery & OEM Background Limits",
                    body = "Some manufacturers (Samsung, Xiaomi, Oppo, Vivo) terminate background accessibility bindings aggressively. Set App Lock to Unrestricted battery usage and lock it in Recents if needed."
                )
                Spacer(modifier = Modifier.height(10.dp))
                BulletSection(
                    title = "4. Platform Limitations",
                    body = "Standard non-root Android apps cannot prevent Force Stop or Uninstall from Android System Settings unless you also lock the Settings app in Manage Apps."
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_open_accessibility")
                ) {
                    Text("Open Accessibility Settings")
                }
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
                    Text("Open App Info (Restricted Settings)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Battery Optimization Settings")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = EmeraldPrimary)
            }
        }
    )
}

@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("About App Lock", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A lightweight, offline-first Android App Locker built with Kotlin, Jetpack Compose, Android Keystore AES-256-GCM, PBKDF2-HMAC-SHA256, and Android BiometricPrompt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = EmeraldPrimary)
            }
        }
    )
}

@Composable
private fun BulletSection(title: String, body: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
