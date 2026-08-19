package com.copiloto.auto

import android.app.Notification
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.regex.Pattern

class MyNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    companion object {
        var lastNotificationAction: Notification.Action? = null
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setPitch(0.9f)
            tts?.setSpeechRate(0.9f)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            tts?.setAudioAttributes(audioAttributes)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = getSharedPreferences("copiloto_prefs", Context.MODE_PRIVATE)
        val isAssistantActive = prefs.getBoolean("is_active", true)
        if (!isAssistantActive) return

        val extras = sbn.notification.extras
        val packageName = sbn.packageName ?: ""

        val isGroup = extras.getBoolean("android.isGroupConversation", false)
        val ignoreGroups = prefs.getBoolean("chk_ignore_groups", true)
        if (ignoreGroups && isGroup) return

        val title = extras.getString("android.title") ?: ""
        var text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (text.isEmpty() && title.isEmpty()) return

        val checkUber = prefs.getBoolean("chk_uber", true)
        val check99 = prefs.getBoolean("chk_99", true)
        val checkWhats = prefs.getBoolean("chk_whats", false)

        val isUber = packageName.contains("ubercab") && checkUber
        val is99 = packageName.contains("taxis99") && check99
        val isWhats = packageName.contains("whatsapp") && checkWhats

        if (isUber || is99 || isWhats) {
            var messageToSpeak: String? = null

            if (isUber || is99) {
                messageToSpeak = processRideOffer(title, text)
            }

            if (messageToSpeak == null) {
                if (text.length > 100) {
                    text = text.take(100) + "... mensagem longa."
                }
                messageToSpeak = "$title disse: $text"
            }

            playBeepSound()
            tts?.speak(messageToSpeak, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    private fun processRideOffer(title: String, text: String): String? {
        val fullContent = "$title $text".lowercase(Locale.getDefault())

        val pricePattern = Pattern.compile("r\\$\\s?(\\d+([.,]\\d{1,2})?)")
        val priceMatcher = pricePattern.matcher(fullContent)

        val distPattern = Pattern.compile("(\\d+([.,]\\d{1,2})?)\\s?km")
        val distMatcher = distPattern.matcher(fullContent)

        if (priceMatcher.find() && distMatcher.find()) {
            try {
                val priceStr = priceMatcher.group(1)?.replace(",", ".") ?: return null
                val distStr = distMatcher.group(1)?.replace(",", ".") ?: return null

                val price = priceStr.toFloat()
                val distance = distStr.toFloat()

                if (distance > 0f) {
                    val rate = price / distance

                    val formattedPrice = String.format(Locale("pt", "BR"), "%.2f", price)
                    val formattedDistance = String.format(Locale("pt", "BR"), "%.1f", distance)
                    val formattedRate = String.format(Locale("pt", "BR"), "%.2f", rate)

                    return "Nova corrida. $formattedPrice reais para $formattedDistance quilômetros. Dá $formattedRate reais por quilômetro."
                }
            } catch (e: Exception) {
                return null
            }
        }
        return null
    }

    private fun playBeepSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
