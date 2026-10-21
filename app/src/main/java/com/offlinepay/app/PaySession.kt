package com.offlinepay.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

sealed interface SState {
    data class Working(val text: String) : SState
    data class NeedPin(val bankMsg: String) : SState
    data class Manual(val msg: String) : SState
    data class Done(val ok: Boolean, val msg: String) : SState
}

/**
 * Runs the whole *99# conversation inside the app. The user never sees the system dialog.
 * The UPI PIN is passed straight to the SIM request and is never stored or logged.
 */
class PaySession(ctx: Context, private val job: UssdJob) {
    private val app = ctx.applicationContext
    private val client = UssdClient(app)
    private val store = TxnStore(app)

    var state by mutableStateOf<SState>(SState.Working(working()))
        private set

    private var steps = 0
    private var started = false
    private var pinSent = false
    private var finished = false

    private fun working() = tr("Bank se baat ho rahi hai…", "बैंक से बात हो रही है…")

    fun start() {
        if (started) return
        started = true
        send("*99#")
    }

    fun reply(text: String) = send(text)

    fun submitPin(pin: String) {
        pinSent = true
        send(pin)
    }

    fun cancel() {
        if (finished) return
        finished = true
        if (job is UssdJob.Pay && !pinSent) store.update(job.txnId, "FAILED", "Cancelled")
    }

    private fun send(text: String) {
        state = SState.Working(working())
        client.send(text, { onResponse(it) }, { onFailure(it) })
    }

    private fun onResponse(msg: String) {
        if (finished) return
        if (++steps > 15) {
            finish(false, tr("Menu samajh nahi aaya. Roka gaya.", "मेन्यू समझ नहीं आया। रोका गया।"))
            return
        }
        when (val a = UssdFlow.decide(job, msg)) {
            is Action.Reply -> send(a.text)
            is Action.Wait ->
                state = if (UssdFlow.pinRe.containsMatchIn(msg)) SState.NeedPin(msg.trim())
                else SState.Manual(msg.trim())
            is Action.Done -> finish(a.ok, a.msg)
            is Action.Abort -> finish(false, a.msg)
        }
    }

    private fun onFailure(reason: String) {
        if (finished) return
        if (pinSent && job is UssdJob.Pay) {
            // Outcome unknown: leave history as PENDING and tell the user to verify.
            finished = true
            state = SState.Done(
                false,
                tr("Result pata nahi chala. Dobara pay karne se pehle bank SMS ya balance check karein.",
                    "नतीजा पता नहीं चला। दोबारा पे करने से पहले बैंक SMS या बैलेंस जाँचें।")
            )
        } else {
            finish(false, reason)
        }
    }

    private fun finish(ok: Boolean, msg: String) {
        finished = true
        if (job is UssdJob.Pay) store.update(job.txnId, if (ok) "SUCCESS" else "FAILED", msg)
        state = SState.Done(ok, msg)
    }
}
