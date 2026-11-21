package com.example

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `manifest enforces offline privacy and disables cloud backup`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("App Lock", context.getString(R.string.app_name))

        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val permissions = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse(
            "App Lock must not request INTERNET permission",
            Manifest.permission.INTERNET in permissions
        )

        val appFlags = context.applicationInfo.flags
        assertEquals(0, appFlags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun `onboarding flow navigates from Welcome to Choose Method and PIN Setup`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("App Lock").assertIsDisplayed()
        composeTestRule.onNodeWithTag("get_started_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Choose Lock Method").assertIsDisplayed()
        composeTestRule.onNodeWithTag("method_pin_card").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("choose_method_continue_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Create PIN").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pin_key_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("pin_key_2").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("pin_key_3").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("pin_key_4").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("pin_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Confirm PIN").performScrollTo().assertIsDisplayed()
    }
}
