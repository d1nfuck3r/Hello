package hello.notify

import android.app.*
import android.content.*
import android.os.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class TTSForegroundService : Service() {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val queue = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private var retryCount = 0

    companion object {
        const val ACTION_SPEAK = "hello.notify.SPEAK"
        const val EXTRA_TEXT   = "text"
        const val CHANNEL_ID   = "notify_tts_channel"
        const val NOTIF_ID     = 1

        fun speak(ctx: Context, text: String) {
            ctx.startService(Intent(ctx, TTSForegroundService::class.java).apply {
                action = ACTION_SPEAK
                putExtra(EXTRA_TEXT, text)
            })
        }
        fun reinit(ctx: Context) {
            ctx.startService(Intent(ctx, TTSForegroundService::class.java).apply {
                action = "REINIT"
            })
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotif())
        initTts()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SPEAK -> intent.getStringExtra(EXTRA_TEXT)?.let { enqueue(it) }
            "REINIT"     -> reinitTts()
        }
        return START_STICKY
    }

    private fun initTts() {
        tts?.shutdown(); tts = null; ttsReady = false
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("th", "TH")
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String) {}
                    override fun onDone(id: String) {}
                    override fun onError(id: String) { handler.postDelayed({ reinitTts() }, 1000) }
                })
                ttsReady = true; retryCount = 0
                queue.forEach { speakNow(it) }; queue.clear()
            } else {
                retryCount++
                if (retryCount < 5) handler.postDelayed({ initTts() }, 5000)
            }
        }
    }

    private fun reinitTts() { retryCount = 0; initTts() }

    private fun enqueue(text: String) {
        if (ttsReady) speakNow(text) else queue.add(text)
    }

    private fun speakNow(text: String) {
        val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "utt_${System.currentTimeMillis()}")
        if (result == TextToSpeech.ERROR) handler.postDelayed({ reinitTts() }, 1000)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Hello Notify", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Service Running..."
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(ch)
        }
    }

    private fun buildNotif(): Notification {
        val pi = PendingIntent.getActivity(this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Hello Notify")
            .setContentText("✓")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pi)
            .build()
    }

    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    override fun onBind(i: Intent?): IBinder? = null
}
