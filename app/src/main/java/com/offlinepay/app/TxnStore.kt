package com.offlinepay.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Txn(
    val id: Long,
    val payee: String,
    val vpa: String,
    val amount: Long,
    val time: Long,
    val status: String, // PENDING | SUCCESS | FAILED
    val detail: String = ""
)

/** Local-only history. UPI PIN is never stored anywhere. */
class TxnStore(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("offlinepay", Context.MODE_PRIVATE)

    @Synchronized
    fun all(): List<Txn> {
        val arr = JSONArray(p.getString("txns", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Txn(
                o.getLong("id"), o.getString("payee"), o.getString("vpa"),
                o.getLong("amount"), o.getLong("time"), o.getString("status"),
                o.optString("detail")
            )
        }.sortedByDescending { it.time }
    }

    @Synchronized
    private fun save(list: List<Txn>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject().put("id", it.id).put("payee", it.payee).put("vpa", it.vpa)
                    .put("amount", it.amount).put("time", it.time)
                    .put("status", it.status).put("detail", it.detail)
            )
        }
        p.edit().putString("txns", arr.toString()).apply()
        UssdSession.version.value = UssdSession.version.value + 1
    }

    fun add(t: Txn) = save(all() + t)

    fun update(id: Long, status: String, detail: String) =
        save(all().map { if (it.id == id) it.copy(status = status, detail = detail.take(300)) else it })

    fun clear() = save(emptyList())
}
