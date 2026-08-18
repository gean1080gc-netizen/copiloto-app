package com.copiloto.auto

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class MyNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    companion object {
        var lastNotificationAction: Notification.Action? = null
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = getSharedPreferences("copiloto_prefs", Context.MODE_PRIVATE)
        val isAssistantActive = prefs.getBoolean("is_active", true)

        if (!isAssistantActive) return

        val packageName = sbn.packageName ?: ""
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        val checkUber = prefs.getBoolean("chk_uber", true)
        val check99 = prefs.getBoolean("chk_99", true)
        val checkWhats = prefs.getBoolean("chk_whats", false)

        val isUber = packageName.contains("ubercab") && checkUber
        val is99 = packageName.contains("taxis99") && check99
        val isWhats = packageName.contains("whatsapp") && checkWhats

        if ((isUber || is99 || isWhats) && text.isNotEmpty()) {
            sbn.notification.actions?.forEach { action ->
                action.remoteInputs?.let { remoteInputs ->
                    if (remoteInputs.isNotEmpty()) {
                        lastNotificationAction = action
                    }
                }
            }

            val fullMessage = "$title disse: $text"
            tts?.speak(fullMessage, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
