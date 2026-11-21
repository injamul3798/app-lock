package com.example.ui

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.LockMethod
import com.example.security.BiometricHelper
import com.example.ui.screens.AboutAppDialog
import com.example.ui.screens.AllSetScreen
import com.example.ui.screens.AutoLockTimeoutDialog
import com.example.ui.screens.ChooseLockMethodScreen
import com.example.ui.screens.CredentialSetupScreen
import com.example.ui.screens.DashboardHomeScreen
import com.example.ui.screens.HelpAndReliabilityDialog
import com.example.ui.screens.PrivacyInfoDialog
import com.example.ui.screens.SelectAppsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.screens.UnlockOverlayScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.CardSurface
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MintContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppLockViewModel

object AppRoutes {
    const val WELCOME = "welcome"
    const val CHOOSE_METHOD = "choose_method"
    const val SETUP_CREDENTIAL = "setup_credential"
    const val ONBOARDING_SELECT_APPS = "onboarding_select_apps"
    const val ALL_SET = "all_set"

    const val HOME = "home"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val MANAGE_APPS = "manage_apps"
    const val CHANGE_PATTERN = "change_pattern"
    const val CHANGE_PIN = "change_pin"
}

private enum class MainTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(AppRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    STATS(AppRoutes.STATS, "Stats", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    SETTINGS(AppRoutes.SETTINGS, "Settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

@Composable
fun AppLockRoot(
    viewModel: AppLockViewModel,
    activity: FragmentActivity? = LocalContext.current as? FragmentActivity
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val appsState by viewModel.appsState.collectAsStateWithLifecycle()
    val biometricCapability by viewModel.biometricCapability.collectAsStateWithLifecycle()
    val isSelfUnlocked by viewModel.isSelfUnlocked.collectAsStateWithLifecycle()

    val onboardingMethod by viewModel.onboardingMethod.collectAsStateWithLifecycle()
    val onboardingBiometric by viewModel.onboardingBiometric.collectAsStateWithLifecycle()
    val onboardingLockedPackages by viewModel.onboardingLockedPackages.collectAsStateWithLifecycle()

    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Gate MainActivity itself when configured and locked
    if (config.isConfigured() && !isSelfUnlocked) {
        WindowSecureEffect(enabled = config.screenCaptureProtection)
        UnlockOverlayScreen(
            config = config,
            targetAppName = "App Lock Vault",
            biometricAvailable = biometricCapability.enrolled,
            initialCooldownSeconds = viewModel.getRemainingCooldownSeconds(),
            onVerifyCredential = { method, chars, callback ->
                viewModel.verifySelfUnlock(method, chars, callback)
            },
            onRequestBiometric = {
                if (activity != null && viewModel.getRemainingCooldownSeconds() == 0) {
                    BiometricHelper.authenticate(
                        activity = activity,
                        title = "Unlock App Lock",
                        subtitle = "Verify your fingerprint to manage protected apps",
                        negativeButtonText = "Use ${config.primaryMethod.label}",
                        onSuccess = { viewModel.onSelfBiometricSuccess() },
                        onFallback = {},
                        onFailureMessage = {}
                    )
                }
            }
        )
        return
    }

    val startDestination = remember(config.isConfigured()) {
        if (config.isConfigured()) AppRoutes.HOME else AppRoutes.WELCOME
    }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomOrRailNav = currentRoute in setOf(
        AppRoutes.HOME,
        AppRoutes.STATS,
        AppRoutes.SETTINGS
    )

    val isSensitiveInputRoute = currentRoute in setOf(
        AppRoutes.SETUP_CREDENTIAL,
        AppRoutes.CHANGE_PATTERN,
        AppRoutes.CHANGE_PIN
    )
    WindowSecureEffect(enabled = config.screenCaptureProtection && isSensitiveInputRoute)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (showBottomOrRailNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = CardSurface,
                        tonalElevation = 4.dp
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(AppRoutes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = MintContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showBottomOrRailNav && useNavigationRail) {
                    NavigationRail(containerColor = CardSurface) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        navController.navigate(tab.route) {
                                            popUpTo(AppRoutes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = MintContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoutes.WELCOME) {
                        WelcomeScreen(
                            onGetStarted = {
                                navController.navigate(AppRoutes.CHOOSE_METHOD)
                            }
                        )
                    }

                    composable(AppRoutes.CHOOSE_METHOD) {
                        BackHandler { navController.popBackStack() }
                        ChooseLockMethodScreen(
                            selectedMethod = onboardingMethod,
                            biometricEnabled = onboardingBiometric,
                            biometricCapability = biometricCapability,
                            onSelectMethod = viewModel::setOnboardingMethod,
                            onToggleBiometric = viewModel::setOnboardingBiometric,
                            onBack = { navController.popBackStack() },
                            onContinue = { navController.navigate(AppRoutes.SETUP_CREDENTIAL) }
                        )
                    }

                    composable(AppRoutes.SETUP_CREDENTIAL) {
                        BackHandler { navController.popBackStack() }
                        CredentialSetupScreen(
                            method = onboardingMethod,
                            isChangeMode = false,
                            onBack = { navController.popBackStack() },
                            onCredentialConfirmed = { chars ->
                                viewModel.stageOnboardingCredential(chars)
                                navController.navigate(AppRoutes.ONBOARDING_SELECT_APPS)
                            }
                        )
                    }

                    composable(AppRoutes.ONBOARDING_SELECT_APPS) {
                        SelectAppsScreen(
                            appsState = appsState,
                            isOnboarding = true,
                            draftLockedPackages = onboardingLockedPackages,
                            loadIcon = viewModel::loadAppIcon,
                            onToggleOnboardingPackage = viewModel::toggleOnboardingPackage,
                            onToggleLiveAppLock = viewModel::toggleInstalledAppLock,
                            onRefreshApps = viewModel::loadInstalledApps,
                            onBack = { navController.popBackStack() },
                            onCompleteSelection = {
                                viewModel.finalizeOnboarding {
                                    navController.navigate(AppRoutes.ALL_SET) {
                                        popUpTo(AppRoutes.WELCOME) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    composable(AppRoutes.ALL_SET) {
                        AllSetScreen(
                            lockedCount = config.lockedPackages.size,
                            health = health,
                            onRefreshHealth = viewModel::refreshHealth,
                            onFinish = {
                                navController.navigate(AppRoutes.HOME) {
                                    popUpTo(AppRoutes.ALL_SET) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(AppRoutes.HOME) {
                        DashboardHomeScreen(
                            config = config,
                            health = health,
                            onToggleMasterProtection = viewModel::setMasterProtection,
                            onManageApps = { navController.navigate(AppRoutes.MANAGE_APPS) },
                            onOpenStats = {
                                navController.navigate(AppRoutes.STATS) {
                                    popUpTo(AppRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onOpenSettings = {
                                navController.navigate(AppRoutes.SETTINGS) {
                                    popUpTo(AppRoutes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onOpenPrivacyInfo = { showPrivacyDialog = true },
                            onOpenHelp = { showHelpDialog = true },
                            onLockVaultNow = { viewModel.lockSelfNow() }
                        )
                    }

                    composable(AppRoutes.STATS) {
                        StatsScreen(
                            config = config,
                            health = health,
                            onClearIntrusionEvents = viewModel::clearIntrusionEvents,
                            onOpenReliabilityHelp = { showHelpDialog = true }
                        )
                    }

                    composable(AppRoutes.SETTINGS) {
                        SettingsScreen(
                            config = config,
                            biometricCapability = biometricCapability,
                            onBack = null,
                            onChangePattern = { navController.navigate(AppRoutes.CHANGE_PATTERN) },
                            onChangePin = { navController.navigate(AppRoutes.CHANGE_PIN) },
                            onSwitchPrimaryMethod = viewModel::setPrimaryMethod,
                            onToggleBiometric = viewModel::setBiometricEnabled,
                            onOpenAutoLockPicker = { showAutoLockDialog = true },
                            onToggleScreenOffLock = viewModel::setLockOnScreenOff,
                            onToggleScreenCaptureProtection = viewModel::setScreenCaptureProtection,
                            onOpenAppProtection = { navController.navigate(AppRoutes.MANAGE_APPS) },
                            onOpenPermissionsHelp = { showHelpDialog = true },
                            onOpenAbout = { showAboutDialog = true }
                        )
                    }

                    composable(AppRoutes.MANAGE_APPS) {
                        SelectAppsScreen(
                            appsState = appsState,
                            isOnboarding = false,
                            draftLockedPackages = config.lockedPackages,
                            loadIcon = viewModel::loadAppIcon,
                            onToggleOnboardingPackage = {},
                            onToggleLiveAppLock = viewModel::toggleInstalledAppLock,
                            onRefreshApps = viewModel::loadInstalledApps,
                            onBack = { navController.popBackStack() },
                            onCompleteSelection = { navController.popBackStack() }
                        )
                    }

                    composable(AppRoutes.CHANGE_PATTERN) {
                        BackHandler { navController.popBackStack() }
                        CredentialSetupScreen(
                            method = LockMethod.PATTERN,
                            isChangeMode = true,
                            onBack = { navController.popBackStack() },
                            onCredentialConfirmed = { chars ->
                                viewModel.updateCredential(LockMethod.PATTERN, chars, makePrimary = true) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }

                    composable(AppRoutes.CHANGE_PIN) {
                        BackHandler { navController.popBackStack() }
                        CredentialSetupScreen(
                            method = LockMethod.PIN,
                            isChangeMode = true,
                            onBack = { navController.popBackStack() },
                            onCredentialConfirmed = { chars ->
                                viewModel.updateCredential(LockMethod.PIN, chars, makePrimary = true) {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAutoLockDialog) {
        AutoLockTimeoutDialog(
            current = config.autoLockTimeout,
            onSelect = {
                viewModel.setAutoLockTimeout(it)
                showAutoLockDialog = false
            },
            onDismiss = { showAutoLockDialog = false }
        )
    }

    if (showPrivacyDialog) {
        PrivacyInfoDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showHelpDialog) {
        HelpAndReliabilityDialog(
            health = health,
            onDismiss = { showHelpDialog = false }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun WindowSecureEffect(enabled: Boolean) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (window != null && enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            if (window != null && enabled) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}
