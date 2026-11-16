package com.elal.demoapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.elal.demoapp.ui.journey.JourneyDemoScreen
import com.elal.demoapp.ui.theme.ElAlDemoTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted or denied — UI will reflect state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissions()
        showActionPlaceholder(intent)

        // TODO: Debug Only firebase — remove before release
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d("FCM_TOKEN", token)
        }

        setContent {
            ElAlDemoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    JourneyDemoScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showActionPlaceholder(intent)
    }

    /** The demo's stand-in for the boarding-pass screen opened from the Live Update action. */
    private fun showActionPlaceholder(intent: Intent?) {
        if (intent?.getStringExtra(DemoActionIntents.EXTRA_PLACEHOLDER) == null) {
            return
        }
        Toast.makeText(this, R.string.demo_boarding_pass_placeholder, Toast.LENGTH_LONG).show()
        intent.removeExtra(DemoActionIntents.EXTRA_PLACEHOLDER)
    }

    /**
     * POST_NOTIFICATIONS is a runtime permission from Android 13 (F4: it was only asked on 16+, so the Android 15
     * fallback could never post); POST_PROMOTED_NOTIFICATIONS only exists from Android 16.
     */
    private fun requestNotificationPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        val permissions = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // POST_PROMOTED_NOTIFICATIONS is required for Live Updates on Android 16
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            try {
                val promotedPermission = "android.permission.POST_PROMOTED_NOTIFICATIONS"
                if (checkSelfPermission(promotedPermission) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(promotedPermission)
                }
            } catch (_: Exception) {
                // Permission may not exist on older platform versions
            }
        }

        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
