package com.ubayanda.milonative

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.MiloApp
import com.ubayanda.milonative.ui.screens.GateScreen
import com.ubayanda.milonative.ui.screens.PermissionScreen
import com.ubayanda.milonative.ui.theme.MiloTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudio() = ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiloTheme { Root() }
        }
    }

    @Composable
    private fun Root() {
        val ctx = LocalContext.current
        var granted by remember { mutableStateOf(hasAudio()) }
        var asked by remember { mutableStateOf(false) }
        var blocked by remember { mutableStateOf(false) }

        val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
        val audio = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            asked = true
            // Android stops showing the box after two refusals; then the only way is Settings
            blocked = !ok && !ActivityCompat.shouldShowRequestPermissionRationale(this, audioPermission)
            if (ok && Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // coming back from Settings: pick up a permission granted there
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (hasAudio()) granted = true }

        val acct by vm.account.collectAsState()
        if (acct.step.isNotEmpty()) {
            GateScreen(vm, acct)
        } else if (granted) {
            LaunchedEffect(Unit) { vm.start() }
            MiloApp(vm)
        } else {
            PermissionScreen(
                blockedForever = asked && blocked,
                onAllow = { audio.launch(audioPermission) },
                onOpenSettings = {
                    ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                },
            )
        }
    }
}
