package hello.notify

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

data class RuleTrigger(
    val keyword  : String = "",
    val template : String = ""
)

data class UserRule(
    val id         : String              = UUID.randomUUID().toString(),
    val appPackage : String              = "",
    val appName    : String              = "",
    val triggers   : List<RuleTrigger>  = emptyList(),
    val enabled    : Boolean             = true
)

object RuleStore {
    private const val PREF = "notify_rules"
    private const val KEY  = "rules_json"
    private val gson = Gson()

    fun load(ctx: Context): List<UserRule> = try {
        val json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return seedIfEmpty(ctx)
        gson.fromJson(json, object : TypeToken<List<UserRule>>() {}.type) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    private fun save(ctx: Context, rules: List<UserRule>) =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString(KEY, gson.toJson(rules)).apply()

    fun seedIfEmpty(ctx: Context): List<UserRule> {
        val existing = try {
            val json = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
            if (json != null) gson.fromJson<List<UserRule>>(json, object : TypeToken<List<UserRule>>() {}.type) else null
        } catch (_: Exception) { null }
        if (!existing.isNullOrEmpty()) return existing
        val defaults = listOf(
            UserRule(appPackage = "jp.naver.line.android", appName = "SCB via LINE",
                triggers = listOf(
                    RuleTrigger("รายการเงินเข้า", "เงินเข้าแล้ว {ยอด}บาท ขอบคุณครับ"),
                    RuleTrigger("รายการเงินออก",  "เงินโอนออก {ยอด}บาท")
                )
            ),
            UserRule(appPackage = "asuk.com.android.app", appName = "7-Eleven",
                triggers = listOf(RuleTrigger("แต้ม", "ได้รับแต้ม 7-Eleven {แต้ม}แต้ม"))
            ),
            UserRule(appPackage = "th.co.truemoney.wallet", appName = "TrueMoney",
                triggers = listOf(RuleTrigger("จ่าย", "จ่ายเงิน TrueMoney {ยอด}บาท"))
            ),
            UserRule(appPackage = "com.linecorp.lineman.driver", appName = "LINE MAN Rider",
                triggers = listOf(RuleTrigger("ออเดอร์ใหม่", "ออเดอร์ใหม่เข้าแล้ว รีบเปิดแอพด่วน"))
            ),
            UserRule(appPackage = "k.bank", appName = "กสิกรไทย",
                triggers = listOf(
                    RuleTrigger("รายการเงินเข้า", "เงินเข้าแล้ว {ยอด}บาท ขอบคุณครับ"),
                    RuleTrigger("รายการเงินออก",  "เงินโอนออก {ยอด}บาท")
                )
            ),
            UserRule(appPackage = "com.scb.phone", appName = "SCB ไทยพาณิชย์",
                triggers = listOf(
                    RuleTrigger("รายการเงินเข้า", "เงินเข้าแล้ว {ยอด}บาท ขอบคุณครับ"),
                    RuleTrigger("รายการเงินออก",  "เงินโอนออก {ยอด}บาท")
                )
            )
        )
        save(ctx, defaults)
        return defaults
    }

    fun upsert(ctx: Context, rule: UserRule) {
        val list = load(ctx).toMutableList()
        val idx  = list.indexOfFirst { it.id == rule.id }
        if (idx >= 0) list[idx] = rule else list.add(rule)
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) = save(ctx, load(ctx).filter { it.id != id })

    fun toggle(ctx: Context, id: String) {
        val list = load(ctx).toMutableList()
        val idx  = list.indexOfFirst { it.id == id }
        if (idx >= 0) list[idx] = list[idx].copy(enabled = !list[idx].enabled)
        save(ctx, list)
    }
}

object LogBus {
    var onLog: ((String) -> Unit)? = null
    fun log(msg: String) { onLog?.invoke(msg) }
}

enum class VAppTab { HOME, SEARCH, INSTALLED, PROFILE, SETTINGS }
