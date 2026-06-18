package hello.notify

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SCBNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg  = sbn.packageName ?: return
        val ctx  = applicationContext
        val rules = RuleStore.load(ctx).filter { it.enabled && it.appPackage == pkg }
        if (rules.isEmpty()) return

        val extras = sbn.notification?.extras ?: return
        val title  = extras.getString("android.title") ?: ""
        val body   = extras.getCharSequence("android.text")?.toString() ?: ""
        val full   = "$title $body"

        LogBus.log("[$pkg] $full")

        for (rule in rules) {
            if (full.contains(rule.keyword, ignoreCase = true)) {
                val vars = VarExtractor.extract(full)
                val text = VarExtractor.format(rule.template, vars)
                LogBus.log("→ SPEAK: $text")
                TTSForegroundService.speak(ctx, text)
                break
            }
        }
    }
}
