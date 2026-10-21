package com.offlinepay.app

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

sealed interface Screen {
    data object Home : Screen
    data object Scan : Screen
    data object Manual : Screen
    data object History : Screen
    data object Settings : Screen
    data object Recharge : Screen
    data class Mobile(val recharge: Boolean) : Screen
    data class Pay(val payee: UpiPayee) : Screen
    data class Session(val job: UssdJob, val subtitle: String) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Payment app: block screenshots / screen recording of PIN and balance screens.
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        Lang.hindi = getSharedPreferences("offlinepay", MODE_PRIVATE).getBoolean("hindi", false)
        setContent { OfflinePayTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val a11y = rememberA11yEnabled()
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var setupComplete by remember { mutableStateOf(ctx.getSharedPreferences("offlinepay", 0).getBoolean("setup_complete", false)) }
    BackHandler(enabled = setupComplete && screen != Screen.Home) { screen = Screen.Home }

    fun compat() = ctx.getSharedPreferences("offlinepay", 0).getBoolean("compat", false)

    fun startBalance() {
        if (compat()) {
            UssdSession.start(UssdJob.Balance)
            try {
                Ussd.dial(ctx)
            } catch (e: Exception) {
                UssdSession.end(tr("Dial nahi ho paya", "डायल नहीं हो पाया"))
            }
        } else {
            screen = Screen.Session(UssdJob.Balance, "")
        }
    }
    val callPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startBalance()
    }
    val onBalance = {
        when {
            compat() && !a11y -> {
                Toast.makeText(ctx, tr("Pehle Accessibility ON karein", "पहले एक्सेसिबिलिटी ON करें"), Toast.LENGTH_LONG).show()
                Ussd.openAccessibilitySettings(ctx)
            }
            !Ussd.hasCallPermission(ctx) -> callPerm.launch(Manifest.permission.CALL_PHONE)
            else -> startBalance()
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!setupComplete) {
            FirstRunSetup(onComplete = { setupComplete = true })
        } else when (val s = screen) {
            Screen.Home -> HomeScreen(go = { screen = it }, onBalance = { onBalance() })
            Screen.Scan -> ScanScreen(onPayee = { screen = Screen.Pay(it) }, onBack = { screen = Screen.Home })
            Screen.Manual -> ManualScreen(onNext = { screen = Screen.Pay(it) }, onBack = { screen = Screen.Home })
            Screen.History -> HistoryScreen(onBack = { screen = Screen.Home })
            Screen.Settings -> SettingsScreen(onBack = { screen = Screen.Home })
            Screen.Recharge -> RechargeScreen(onNext = { screen = Screen.Mobile(true) }, onBack = { screen = Screen.Home })
            is Screen.Mobile -> MobileScreen(s.recharge, onNext = { screen = Screen.Pay(it) }, onBack = { screen = Screen.Home })
            is Screen.Pay -> PayScreen(
                s.payee,
                onBack = { screen = Screen.Home },
                onSession = { job, sub -> screen = Screen.Session(job, sub) },
                onCompatStarted = { screen = Screen.Home }
            )
            is Screen.Session -> SessionScreen(s.job, s.subtitle, onClose = { screen = Screen.Home })
        }
    }
}
