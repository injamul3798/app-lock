package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.AppLockRoot
import com.example.ui.theme.AppLockTheme
import com.example.ui.viewmodel.AppLockViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: AppLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> viewModel.onMainActivityStarted()
                    Lifecycle.Event.ON_RESUME -> viewModel.refreshHealth()
                    Lifecycle.Event.ON_STOP -> viewModel.onMainActivityStopped()
                    else -> Unit
                }
            }
        )

        setContent {
            AppLockTheme {
                AppLockRoot(
                    viewModel = viewModel,
                    activity = this
                )
            }
        }
    }
}
