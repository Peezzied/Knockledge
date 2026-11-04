package group.two.knockledge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import group.two.knockledge.ui.LiveTestAppShell
import group.two.knockledge.ui.SplashOverlay
import group.two.knockledge.ui.theme.KnockledgeTheme

/**
 * Single-activity Material 3 live test UI shell entrypoint.
 * Hosts [LiveTestAppShell] and handles [Manifest.permission.RECORD_AUDIO] and
 * [Manifest.permission.POST_NOTIFICATIONS] permissions. Shows the system splash, then hands
 * off to [SplashOverlay] for the brand hold once the first app frame is composed.
 */
class MainActivity : ComponentActivity() {
    private var hasRecordAudioPermission by mutableStateOf(false)
    private var firstFrameDrawn by mutableStateOf(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.RECORD_AUDIO]
            ?: (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        hasRecordAudioPermission = audioGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !firstFrameDrawn }
        enableEdgeToEdge()

        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

        hasRecordAudioPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

        val requestMissingPermissions = {
            val ungranted = requiredPermissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()

            if (ungranted.isNotEmpty()) {
                requestPermissionsLauncher.launch(ungranted)
            }
        }

        requestMissingPermissions()

        setContent {
            KnockledgeTheme {
                val app = application as KnockledgeApplication
                val splashDone = rememberSaveable { mutableStateOf(false) }
                Box(Modifier.fillMaxSize()) {
                    LiveTestAppShell(
                        component = app.component,
                        hasRecordAudioPermission = hasRecordAudioPermission,
                        onRequestPermission = requestMissingPermissions,
                    )
                    if (!splashDone.value) {
                        SplashOverlay(
                            animationsEnabled = Settings.Global.getFloat(
                                contentResolver,
                                Settings.Global.ANIMATOR_DURATION_SCALE,
                                1f,
                            ) != 0f,
                            onFirstFrame = { firstFrameDrawn = true },
                            onFinished = { splashDone.value = true },
                        )
                    }
                }
            }
        }
    }
}

