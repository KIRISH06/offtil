package com.offlinepay.app

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor

val Purple = Color(0xFF5F259F)
val PurpleDark = Color(0xFF3B1470)
val Green = Color(0xFF1B9E5A)
val Red = Color(0xFFD93025)
const val MAX_AMOUNT = 5000L

@Composable
fun OfflinePayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple, secondary = Green,
            background = Color(0xFFF6F3FA), surface = Color.White
        ),
        content = content
    )
}

@Composable
fun rememberA11yEnabled(): Boolean {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(Ussd.isServiceEnabled(ctx)) }
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) enabled = Ussd.isServiceEnabled(ctx)
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    return enabled
}

@Composable
fun BackRow(title: String, onBack: () -> Unit, light: Boolean = false) {
    val c = if (light) Color.White else Color(0xFF1C1B1F)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("←", color = c, fontSize = 24.sp,
            modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onBack() }.padding(horizontal = 12.dp, vertical = 4.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = c, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun BigAction(icon: String, label: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 34.sp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
    }
}

// ---------------------------------------------------------------- HOME
@Composable
fun HomeScreen(go: (Screen) -> Unit, onBalance: () -> Unit) {
    val ctx = LocalContext.current
    val a11y = rememberA11yEnabled()
    val compat = ctx.getSharedPreferences("offlinepay", 0).getBoolean("compat", false)
    val status by UssdSession.status.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            Modifier.fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(PurpleDark, Purple)),
                    RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ).padding(24.dp)
        ) {
            Column {
                Text("OfflinePay", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    tr("Bina internet payment • *99# se", "बिना इंटरनेट पेमेंट • *99# से"),
                    color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    tr("Limit ₹$MAX_AMOUNT per transaction • SIM signal chahiye",
                        "सीमा ₹$MAX_AMOUNT प्रति लेनदेन • SIM सिग्नल चाहिए"),
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp
                )
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (compat && !a11y) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
                    Column(Modifier.padding(16.dp)) {
                        Text(tr("Ek baar setup zaroori hai", "एक बार सेटअप ज़रूरी है"), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tr("Accessibility mein 'OfflinePay' ON karein, taaki app *99# menu khud chala sake. Aapka UPI PIN app kabhi nahi padhta.",
                                "एक्सेसिबिलिटी में 'OfflinePay' ON करें ताकि ऐप *99# मेन्यू खुद चला सके। आपका UPI PIN ऐप कभी नहीं पढ़ता।"),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { Ussd.openAccessibilitySettings(ctx) }) {
                            Text(tr("Settings kholein", "सेटिंग्स खोलें"))
                        }
                    }
                }
            }

            if (status.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(status, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("✕", modifier = Modifier.clickable { UssdSession.status.value = "" }.padding(8.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigAction("📷", tr("Scan & Pay", "स्कैन करें"), Modifier.weight(1f)) { go(Screen.Scan) }
                BigAction("✍️", tr("UPI ID se Pay", "UPI ID से पे"), Modifier.weight(1f)) { go(Screen.Manual) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigAction("💰", tr("Balance", "बैलेंस"), Modifier.weight(1f)) { onBalance() }
                BigAction("🧾", tr("History", "हिस्ट्री"), Modifier.weight(1f)) { go(Screen.History) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BigAction("📱", tr("Mobile number se pay", "मोबाइल नंबर से पे"), Modifier.weight(1f)) { go(Screen.Mobile(false)) }
            }
            Text(
                "⚙️  " + tr("Settings & Help", "सेटिंग्स और मदद"),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable { go(Screen.Settings) }.padding(16.dp),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------------- MANUAL
@Composable
fun ManualScreen(onNext: (UpiPayee) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var vpa by remember { mutableStateOf("") }
    val recents = remember { TxnStore(ctx).all().filter { UpiParser.isValidVpa(it.vpa) }.distinctBy { it.vpa }.take(5) }
    val valid = UpiParser.isValidVpa(vpa)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(tr("UPI ID se Pay", "UPI ID से पे"), onBack)
        OutlinedTextField(
            value = vpa, onValueChange = { vpa = it.trim().lowercase() },
            label = { Text("UPI ID (name@bank)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            isError = vpa.isNotEmpty() && !valid
        )
        if (recents.isNotEmpty()) {
            Text(tr("Recent", "हाल के"), fontWeight = FontWeight.SemiBold)
            recents.forEach {
                Text(
                    "${it.payee}  •  ${it.vpa}",
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color.White).clickable { vpa = it.vpa }.padding(14.dp),
                    fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onNext(UpiPayee(vpa, vpa)) }, enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(tr("Aage badhein", "आगे बढ़ें")) }
    }
}

// ---------------------------------------------------------------- MOBILE / RECHARGE
@Composable
fun MobileScreen(recharge: Boolean, onNext: (UpiPayee) -> Unit, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var num by remember { mutableStateOf("") }
    val mobileRe = remember { Regex("^[6-9]\\d{9}$") }
    val valid = mobileRe.matches(num)
    val recents = remember {
        TxnStore(ctx).all().filter { mobileRe.matches(it.vpa) }.distinctBy { it.vpa }.take(5)
    }
    val name = if (recharge) tr("Recharge dukaandaar", "रिचार्ज दुकानदार") else tr("Mobile number", "मोबाइल नंबर")

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(if (recharge) tr("Recharge", "रिचार्ज") else tr("Mobile number se pay", "मोबाइल नंबर से पे"), onBack)
        if (recharge) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
                Text(
                    tr("Recharge karne wale dukaandaar ka number daalein. Payment ke baad unhe apna number bata dein.",
                        "रिचार्ज करने वाले दुकानदार का नंबर डालें। पेमेंट के बाद उन्हें अपना नंबर बता दें."),
                    Modifier.padding(14.dp), fontSize = 14.sp
                )
            }
        }
        OutlinedTextField(
            value = num, onValueChange = { num = it.filter(Char::isDigit).take(10) },
            label = { Text(tr("10 digit mobile number", "10 अंकों का मोबाइल नंबर")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            isError = num.length == 10 && !valid,
            supportingText = {
                Text(tr("UPI ID bank khud dhoondh leta hai", "UPI ID बैंक खुद ढूँढ लेता है"))
            }
        )
        recents.forEach {
            Text(
                "${it.vpa}  •  ${it.payee}",
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color.White).clickable { num = it.vpa }.padding(14.dp),
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { onNext(UpiPayee(num, name, mobile = true)) }, enabled = valid,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(tr("Aage badhein", "आगे बढ़ें")) }
    }
}

@Composable
fun RechargeScreen(onNext: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BackRow(tr("Recharge", "रिचार्ज"), onBack)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text(tr("Offline recharge kaise karein", "ऑफ़लाइन रिचार्ज कैसे करें"), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("*99# mein recharge ya bill-pay ka option nahi hota, isliye app seedha operator ko recharge nahi kar sakta. Sabse aasaan tarika: recharge karne wale dukaandaar ko mobile number se pay karein, wo aapke number par recharge kar dega.",
                        "*99# में रिचार्ज या बिल-पे का ऑप्शन नहीं होता, इसलिए ऐप सीधे ऑपरेटर को रिचार्ज नहीं कर सकता। सबसे आसान तरीका: रिचार्ज करने वाले दुकानदार को मोबाइल नंबर से पे करें, वह आपके नंबर पर रिचार्ज कर देगा।"),
                    fontSize = 14.sp, color = Color.DarkGray
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text(tr("Dukaandaar ko pay karein", "दुकानदार को पे करें"))
        }
    }
}

// ---------------------------------------------------------------- PAY
@Composable
fun PayScreen(
    payee: UpiPayee,
    onBack: () -> Unit,
    onSession: (UssdJob, String) -> Unit,
    onCompatStarted: () -> Unit
) {
    val ctx = LocalContext.current
    val a11y = rememberA11yEnabled()
    val compat = ctx.getSharedPreferences("offlinepay", 0).getBoolean("compat", false)
    val qrAmount = payee.amount?.toDoubleOrNull()?.takeIf { it == floor(it) }?.toLong()
    var amt by remember { mutableStateOf(qrAmount?.toString() ?: "") }
    var confirm by remember { mutableStateOf(false) }
    val value = amt.toLongOrNull()
    val amountOk = value != null && value in 1..MAX_AMOUNT
    val shownId = if (payee.mobile) "+91 ${payee.vpa}" else payee.vpa

    fun launchPay() {
        val v = value ?: return
        val id = System.currentTimeMillis()
        TxnStore(ctx).add(Txn(id, payee.name, payee.vpa, v, id, "PENDING"))
        val job = UssdJob.Pay(payee.vpa, v.toString(), id, payee.mobile)
        if (compat) {
            UssdSession.start(job)
            try {
                Ussd.dial(ctx)
                onCompatStarted()
            } catch (e: Exception) {
                UssdSession.end(tr("Dial nahi ho paya", "डायल नहीं हो पाया"))
                TxnStore(ctx).update(id, "FAILED", "Dial failed")
                Toast.makeText(ctx, e.message ?: "Error", Toast.LENGTH_LONG).show()
            }
        } else {
            onSession(job, "₹$v → ${payee.name}\n$shownId")
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) launchPay()
        else Toast.makeText(ctx, tr("Call permission chahiye", "कॉल अनुमति चाहिए"), Toast.LENGTH_LONG).show()
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackRow(tr("Payment", "पेमेंट"), onBack)
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp)) {
                Text(tr("Paying to", "भुगतान किसे"), fontSize = 12.sp, color = Color.Gray)
                Text(payee.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(shownId, fontSize = 14.sp, color = Color.DarkGray)
            }
        }
        OutlinedTextField(
            value = amt, onValueChange = { amt = it.filter(Char::isDigit).take(5) },
            label = { Text(tr("Amount (₹)", "राशि (₹)")) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            isError = amt.isNotEmpty() && !amountOk,
            supportingText = {
                Text(
                    if (payee.amount != null && qrAmount == null)
                        tr("QR amount decimal hai; *99# par poore rupaye hi chalte hain", "QR राशि दशमलव में है; *99# पर पूरे रुपये ही चलते हैं")
                    else tr("₹1 se ₹$MAX_AMOUNT tak", "₹1 से ₹$MAX_AMOUNT तक")
                )
            }
        )
        Spacer(Modifier.weight(1f))
        Text(
            tr("Payment confirm karne ke liye bank ka official USSD/UPI screen use karein. Apna UPI PIN sirf official bank/UPI screen par enter karein.",
                "पेमेंट कन्फ़र्म करने के लिए बैंक की आधिकारिक USSD/UPI स्क्रीन इस्तेमाल करें। अपना UPI PIN सिर्फ आधिकारिक बैंक/UPI स्क्रीन पर डालें।"),
            fontSize = 12.sp, color = Color.Gray
        )
        Button(
            onClick = { confirm = true }, enabled = amountOk,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green)
        ) { Text(tr("Pay via *99#", "*99# से पे करें"), fontSize = 16.sp) }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(tr("Confirm payment", "पेमेंट कन्फ़र्म करें")) },
            text = { Text("₹$amt → ${payee.name}\n$shownId") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    when {
                        compat && !a11y -> {
                            Toast.makeText(ctx, tr("Pehle Accessibility ON karein", "पहले एक्सेसिबिलिटी ON करें"), Toast.LENGTH_LONG).show()
                            Ussd.openAccessibilitySettings(ctx)
                        }
                        !Ussd.hasCallPermission(ctx) -> permLauncher.launch(Manifest.permission.CALL_PHONE)
                        else -> launchPay()
                    }
                }) { Text(tr("Pay", "पे करें")) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(tr("Cancel", "रद्द")) } }
        )
    }
}

// ---------------------------------------------------------------- HISTORY
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val v by UssdSession.version.collectAsState()
    val list = remember(v) { TxnStore(ctx).all() }
    val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { BackRow(tr("History", "हिस्ट्री"), onBack) }
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(tr("Abhi koi transaction nahi", "अभी कोई लेनदेन नहीं"), color = Color.Gray)
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEach { t ->
                    val color = when (t.status) { "SUCCESS" -> Green; "FAILED" -> Red; else -> Color(0xFFF9A825) }
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(t.payee, fontWeight = FontWeight.SemiBold)
                                Text(t.vpa, fontSize = 12.sp, color = Color.Gray)
                                Text(fmt.format(Date(t.time)), fontSize = 12.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${t.amount}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(t.status, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- SETTINGS
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val a11y = rememberA11yEnabled()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BackRow(tr("Settings & Help", "सेटिंग्स और मदद"), onBack)

        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("हिंदी", modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                Switch(checked = Lang.hindi, onCheckedChange = {
                    Lang.hindi = it
                    ctx.getSharedPreferences("offlinepay", 0).edit().putBoolean("hindi", it).apply()
                })
            }
        }
        var compat by remember { mutableStateOf(ctx.getSharedPreferences("offlinepay", 0).getBoolean("compat", false)) }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Compatibility mode", "कम्पैटिबिलिटी मोड"), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Switch(checked = compat, onCheckedChange = {
                        compat = it
                        ctx.getSharedPreferences("offlinepay", 0).edit().putBoolean("compat", it).apply()
                    })
                }
                Text(
                    tr("Sirf tab ON karein jab app ke andar payment na chale. Tab system dialog use hota hai.",
                        "सिर्फ़ तब ON करें जब ऐप के अंदर पेमेंट न चले। तब सिस्टम डायलॉग इस्तेमाल होता है।"),
                    fontSize = 12.sp, color = Color.Gray
                )
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                Text("Accessibility: " + if (a11y) "ON ✅" else "OFF ❌", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { Ussd.openAccessibilitySettings(ctx) }) {
                    Text(tr("Accessibility settings", "एक्सेसिबिलिटी सेटिंग्स"))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Yeh kaise kaam karta hai", "यह कैसे काम करता है"), fontWeight = FontWeight.Bold)
                Text(tr(
                    "1. QR offline scan hota hai.\n2. App *99# dial karta hai (internet nahi, sirf SIM signal).\n3. Menu, UPI ID aur amount app khud bhar deta hai, sab kuch app ke andar.\n4. UPI PIN aap isi app ke PIN box mein daalte ho.\n5. Result history mein save hota hai.",
                    "1. QR ऑफ़लाइन स्कैन होता है।\n2. ऐप *99# डायल करता है (इंटरनेट नहीं, सिर्फ़ SIM सिग्नल)।\n3. मेन्यू, UPI ID और राशि ऐप खुद भर देता है, सब कुछ ऐप के अंदर।\n4. UPI PIN आप इसी ऐप के PIN बॉक्स में डालते हैं।\n5. नतीजा हिस्ट्री में सेव होता है।"
                ), fontSize = 13.sp)
                Text(tr(
                    "Dhyan rakhein: SIM mein mobile signal aur bank-linked number zaroori. Mobile number wahi ho jo bank mein registered hai. Dual SIM mein default calling SIM wahi rakhein.",
                    "ध्यान रखें: SIM में मोबाइल सिग्नल और बैंक से जुड़ा नंबर ज़रूरी। नंबर वही हो जो बैंक में रजिस्टर्ड है। डुअल SIM में वही SIM डिफ़ॉल्ट कॉलिंग रखें।"
                ), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}


// First-launch setup. SIM selection is stored as a preference; Android/operator support
// determines whether a particular SIM can be selected for USSD on a given device.
@Composable
fun FirstRunSetup(onComplete: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("offlinepay", 0)
    var sim by remember { mutableStateOf(prefs.getString("preferred_sim", "system") ?: "system") }
    var permissionsRead by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(20.dp))
        Text("OfflinePay", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Purple)
        Text(tr("Welcome — let's set up your app", "स्वागत है — ऐप सेटअप करें"), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text(tr("1. Choose which SIM you prefer for USSD. Some phones may still use the system's default calling SIM.", "1. USSD के लिए अपनी पसंद की SIM चुनें। कुछ फोन में सिस्टम की डिफ़ॉल्ट कॉलिंग SIM ही इस्तेमाल होगी।"), fontSize = 14.sp)
        listOf("system" to tr("Ask / system default", "सिस्टम की डिफ़ॉल्ट SIM"), "sim1" to "SIM 1", "sim2" to "SIM 2").forEach { (value, label) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).clickable { sim = value }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = sim == value, onClick = { sim = value })
                Spacer(Modifier.width(8.dp)); Text(label)
            }
        }
        Text(tr("2. Permissions", "2. अनुमतियाँ"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(tr("Camera is needed only to scan a payment QR. Phone permission may be requested for USSD. Grant only permissions you understand; bank confirmation must remain visible.", "पेमेंट QR स्कैन करने के लिए कैमरा चाहिए। USSD के लिए फ़ोन अनुमति माँगी जा सकती है। केवल समझ में आने वाली अनुमति दें; बैंक की पुष्टि स्क्रीन पर दिखाई देनी चाहिए।"), fontSize = 14.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = permissionsRead, onCheckedChange = { permissionsRead = it })
            Text(tr("I have read the permission guide", "मैंने अनुमति गाइड पढ़ ली है"), fontSize = 14.sp)
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = {
            prefs.edit().putString("preferred_sim", sim).putBoolean("setup_complete", true).apply()
            onComplete()
        }, enabled = permissionsRead, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(tr("Finish setup", "सेटअप पूरा करें"))
        }
    }
}
