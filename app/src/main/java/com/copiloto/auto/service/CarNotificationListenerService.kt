package com.copiloto.auto.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.regex.Pattern

class CarNotificationListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "BR"))
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsReady = true
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || !isTtsReady) return

        val packageName = sbn.packageName ?: ""
        if (!isRideApp(packageName)) return

        val extras: Bundle = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val fullContent = "$title $text"

        val value = extractValue(fullContent)
        val km = extractKm(fullContent)

        if (value != null && km != null && km > 0f) {
            val rate = value / km
            val message = String.format(
                Locale("pt", "BR"),
                "Nova corrida. %.2f reais para %.1f quilômetros. Dá %.2f reais por quilômetro.",
                value, km, rate
            )
            falar(message)
        } else if (text.isNotBlank()) {
            falar(text)
        }
    }

    private fun isRideApp(packageName: String): Boolean {
        return packageName.contains("ubercab", ignoreCase = true) ||
               packageName.contains("taxis99", ignoreCase = true) ||
               packageName.contains("driver", ignoreCase = true)
    }

    private fun extractValue(content: String): Float? {
        val pattern = Pattern.compile("""R\$\s*([\d]+(?:[\.,]\d+)?)""")
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(".", "")?.replace(",", ".")
            return raw?.toFloatOrNull()
        }
        return null
    }

    private fun extractKm(content: String): Float? {
        val pattern = Pattern.compile("""([\d]+(?:[\.,]\d+)?)\s*(?:km|KM|quilômetros|quilometros)""")
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            val raw = matcher.group(1)?.replace(".", "")?.replace(",", ".")
            return raw?.toFloatOrNull()
        }
        return null
    }

    private fun falar(texto: String) {
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "COPILOTO_TTS_ID")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
