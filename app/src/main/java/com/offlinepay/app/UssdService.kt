package com.offlinepay.app

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Active ONLY while a payment/balance job is running. Reads the system USSD dialog,
 * types menu/amount/UPI ID. EditText contents are never read, so the PIN stays private.
 */
class UssdService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val task = Runnable { process() }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (UssdSession.job == null) return
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName || !isDialerPkg(pkg)) return
        handler.removeCallbacks(task)
        handler.postDelayed(task, 400)
    }

    private fun isDialerPkg(p: String) =
        listOf("phone", "telephony", "dialer", "incallui", "telecom").any { p.contains(it, true) }

    private fun process() {
        val job = UssdSession.job ?: return
        if (System.currentTimeMillis() - UssdSession.startedAt > 180_000) {
            UssdSession.end(tr("Time out ho gaya. Dobara try karein.", "टाइम आउट। दोबारा कोशिश करें।"))
            return
        }
        val root = rootInActiveWindow ?: return
        if (!isDialerPkg(root.packageName?.toString().orEmpty())) return

        val texts = ArrayList<String>()
        val buttons = ArrayList<AccessibilityNodeInfo>()
        val inputs = ArrayList<AccessibilityNodeInfo>()
        walk(root, texts, buttons, inputs)

        val msg = texts.joinToString("\n")
        if (msg.isBlank() || msg == UssdSession.lastMsg) return
        UssdSession.lastMsg = msg

        when (val a = UssdFlow.decide(job, msg)) {
            is Action.Reply -> {
                if (++UssdSession.steps > 12) {
                    click(buttons, listOf("cancel", "ok", "dismiss"))
                    UssdSession.end(tr("Menu samajh nahi aaya. Roka gaya.", "मेन्यू समझ नहीं आया। रोका गया।"))
                    return
                }
                val input = inputs.firstOrNull()
                if (input == null) {
                    UssdSession.status.value = tr("Dialog dekhein", "डायलॉग देखें")
                    return
                }
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, a.text)
                }
                input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                UssdSession.status.value = tr("Step ${UssdSession.steps} bhej diya…", "स्टेप ${UssdSession.steps} भेजा…")
                handler.postDelayed({
                    click(buttons, listOf("send", "ok", "reply", "submit", "yes"), fallbackLast = true)
                }, 250)
            }

            is Action.Wait -> UssdSession.status.value = a.hint

            is Action.Done -> {
                if (job is UssdJob.Pay) {
                    TxnStore(applicationContext).update(
                        job.txnId, if (a.ok) "SUCCESS" else "FAILED", a.msg
                    )
                }
                click(buttons, listOf("ok", "cancel", "dismiss", "close"))
                UssdSession.end(a.msg)
            }

            is Action.Abort -> {
                click(buttons, listOf("cancel", "ok", "dismiss"))
                UssdSession.end(a.msg)
            }
        }
    }

    private fun walk(
        n: AccessibilityNodeInfo?,
        texts: MutableList<String>,
        buttons: MutableList<AccessibilityNodeInfo>,
        inputs: MutableList<AccessibilityNodeInfo>
    ) {
        if (n == null) return
        val cls = n.className?.toString().orEmpty()
        when {
            cls.endsWith("EditText") -> inputs.add(n)
            cls.endsWith("Button") -> buttons.add(n)
            cls.endsWith("TextView") ->
                n.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        }
        for (i in 0 until n.childCount) walk(n.getChild(i), texts, buttons, inputs)
    }

    private fun click(
        buttons: List<AccessibilityNodeInfo>,
        names: List<String>,
        fallbackLast: Boolean = false
    ) {
        for (name in names) {
            buttons.firstOrNull { it.text?.toString()?.trim().equals(name, true) }?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK); return
            }
        }
        (if (fallbackLast) buttons.lastOrNull() else buttons.firstOrNull())
            ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }
}
