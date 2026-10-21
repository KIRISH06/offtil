package com.offlinepay.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow

sealed interface UssdJob {
    data class Pay(val target: String, val amount: String, val txnId: Long, val mobile: Boolean = false) : UssdJob
    data object Balance : UssdJob
}

object UssdSession {
    @Volatile var job: UssdJob? = null
    @Volatile var steps = 0
    @Volatile var lastMsg = ""
    @Volatile var startedAt = 0L

    val status = MutableStateFlow("")
    val version = MutableStateFlow(0)

    fun start(j: UssdJob) {
        job = j
        steps = 0
        lastMsg = ""
        startedAt = System.currentTimeMillis()
        status.value = tr("*99# dial ho raha hai…", "*99# डायल हो रहा है…")
    }

    fun end(msg: String) {
        job = null
        status.value = msg
    }
}

object Ussd {
    fun hasCallPermission(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

    fun isServiceEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val cn = ComponentName(ctx, UssdService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }

    fun openAccessibilitySettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Dials *99#. The accessibility service then drives the menu. */
    fun dial(ctx: Context) {
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode("*99#")))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(intent)
    }
}
