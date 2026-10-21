package com.offlinepay.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SessionScreen(job: UssdJob, subtitle: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val session = remember { PaySession(ctx, job) }
    LaunchedEffect(Unit) { session.start() }
    val close = { session.cancel(); onClose() }
    BackHandler { close() }

    val title = if (job is UssdJob.Balance) tr("Balance", "बैलेंस") else tr("Payment", "पेमेंट")

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        BackRow(title, close)
        if (subtitle.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Text(subtitle, Modifier.padding(16.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        when (val s = session.state) {
            is SState.Working -> Column(
                Modifier.fillMaxWidth().padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Purple)
                Text(s.text, color = Color.Gray)
            }

            is SState.NeedPin -> {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(tr("Bank confirmation required", "बैंक की पुष्टि ज़रूरी है"), fontWeight = FontWeight.Bold)
                        Text(tr("For your safety, OfflinePay will not collect or submit your UPI PIN. Enter it only on the bank's official USSD/UPI screen. Verify the recipient and amount before approving.", "आपकी सुरक्षा के लिए OfflinePay आपका UPI PIN नहीं लेगा या भेजेगा। PIN केवल बैंक की आधिकारिक USSD/UPI स्क्रीन पर डालें। मंज़ूरी से पहले पाने वाले और राशि की जाँच करें।"))
                        Text(s.bankMsg, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
                Button(onClick = close, modifier = Modifier.fillMaxWidth()) { Text(tr("Close and verify with bank", "बंद करें और बैंक से पुष्टि करें")) }
            }

            is SState.Manual -> ManualPanel(s.msg, onReply = { session.reply(it) }, onClose = close)

            is SState.Done -> {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (s.ok) "✅" else "❌", fontSize = 56.sp)
                    Text(
                        if (s.ok) tr("Ho gaya", "हो गया") else tr("Nahi hua", "नहीं हुआ"),
                        fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = if (s.ok) Green else Red
                    )
                    Text(s.msg, color = Color.DarkGray)
                }
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text(tr("Done", "ठीक है"))
                }
            }
        }
    }
}

@Composable
private fun ManualPanel(msg: String, onReply: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val opts = remember(msg) { UssdFlow.options(msg) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Text(msg, Modifier.padding(16.dp), fontSize = 15.sp)
    }
    if (opts.isNotEmpty()) {
        opts.forEach { (num, label) ->
            OutlinedButton(onClick = { onReply(num) }, modifier = Modifier.fillMaxWidth()) {
                Text("$num. $label")
            }
        }
    } else {
        OutlinedTextField(
            value = text, onValueChange = { text = it }, singleLine = true,
            label = { Text(tr("Jawab likhein", "जवाब लिखें")) },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { val t = text; text = ""; onReply(t) },
            enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(tr("Bhejein", "भेजें")) }
    }
    TextButton(onClick = onClose) { Text(tr("Band karein", "बंद करें")) }
}
