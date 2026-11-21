package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppsProvider(private val context: Context) {

    private val iconCache = LruCache<String, ImageBitmap>(80)

    suspend fun loadLaunchableApps(lockedPackages: Set<String>): List<InstalledAppInfo> =
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)

            val ownPkg = context.packageName
            val launcherPkgs = getLauncherPackages()

            val appsMap = LinkedHashMap<String, InstalledAppInfo>()
            for (resolveInfo in resolved) {
                val activityInfo = resolveInfo.activityInfo ?: continue
                val pkg = activityInfo.packageName ?: continue
                if (pkg == ownPkg || pkg in launcherPkgs || appsMap.containsKey(pkg)) continue

                val label = runCatching {
                    resolveInfo.loadLabel(pm).toString().trim()
                }.getOrDefault(pkg).ifBlank { pkg }

                val appInfo = activityInfo.applicationInfo
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                appsMap[pkg] = InstalledAppInfo(
                    packageName = pkg,
                    label = label,
                    isSystemApp = isSystem,
                    isLocked = pkg in lockedPackages
                )
            }

            // Also allow protecting Android Settings if visible via SETTINGS intent
            val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS)
            val settingsResolved = pm.resolveActivity(settingsIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val settingsPkg = settingsResolved?.activityInfo?.packageName
            if (settingsPkg != null && settingsPkg != ownPkg && !appsMap.containsKey(settingsPkg)) {
                val settingsLabel = runCatching {
                    settingsResolved.loadLabel(pm).toString().trim()
                }.getOrDefault("Settings").ifBlank { "Settings" }
                appsMap[settingsPkg] = InstalledAppInfo(
                    packageName = settingsPkg,
                    label = settingsLabel,
                    isSystemApp = true,
                    isLocked = settingsPkg in lockedPackages
                )
            }

            appsMap.values.sortedWith(
                compareByDescending<InstalledAppInfo> { it.isLocked }
                    .thenBy { it.label.lowercase() }
            )
        }

    suspend fun loadAppIcon(packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        iconCache.get(packageName)?.let { return@withContext it }
        runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable).asImageBitmap()
            iconCache.put(packageName, bitmap)
            bitmap
        }.getOrNull()
    }

    fun getAppLabel(packageName: String): String {
        return runCatching {
            val info = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(info).toString().trim()
        }.getOrDefault(packageName).ifBlank { packageName }
    }

    fun getLauncherPackages(): Set<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return runCatching {
            context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { it.activityInfo?.packageName }
                .toSet()
        }.getOrDefault(emptySet())
    }

    fun getInputMethodPackages(): Set<String> {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return emptySet()
        return runCatching {
            imm.enabledInputMethodList.mapNotNull { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = drawable.intrinsicWidth.coerceIn(48, 128)
        val height = drawable.intrinsicHeight.coerceIn(48, 128)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
