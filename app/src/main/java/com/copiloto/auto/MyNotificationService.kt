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

        // Ignora SMS / Códigos de Confirmação / PIN
        val hasCodeWord = text.contains("código", ignoreCase = true) ||
                          text.contains("pin", ignoreCase = true) ||
                          text.contains("verificação", ignoreCase = true)
        val hasNumbersOnly = text.contains(Regex("\\b\\d{4,6}\\b"))

        if (hasCodeWord || hasNumbersOnly) return

        val checkUber = prefs.getBoolean("chk_uber", true)
        val check99 = prefs.getBoolean("chk_99", true)
        val checkWhats = prefs.getBoolean("chk_whats", false)

        val isUber = packageName.contains("ubercab") && checkUber
        val is99 = packageName.contains("taxis99") && check99
        val isWhats = packageName.contains("whatsapp") && checkWhats

        if (isUber || is99 || isWhats) {
            sbn.notification.actions?.forEach { action ->
                action.remoteInputs?.let { remoteInputs ->
                    if (remoteInputs.isNotEmpty()) {
                        lastNotificationAction = action
                    }
                }
            }

            var messageToSpeak: String? = null

            // Se for Uber ou 99, tenta extrair os valores e calcular R$/KM
            if (isUber || is99) {
                messageToSpeak = processRideOffer(title, text)
            }

            // Se não for oferta de corrida ou for mensagem comum (WhatsApp / Chat)
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

    // Função que extrai os valores da notificação e calcula o ganho por KM
    private fun processRideOffer(title: String, text: String): String? {
        val fullContent = "$title $text"

        // Busca por valores em Reais (ex: R$ 25,00 / R$ 25 / R$25.50)
        val priceRegex = Regex("""R\$\s?(\d+([.,]\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val priceMatch = priceRegex.find(fullContent)

        // Busca por distância em KM (ex: 8,5 km / 8.5km / 8 km)
        val distanceRegex = Regex("""(\d+([.,]\d{1,2})?)\s?km""", RegexOption.IGNORE_CASE)
        val distanceMatch = distanceRegex.find(fullContent)

        if (priceMatch != null && distanceMatch != null) {
            try {
                val priceStr = priceMatch.groupValues[1].replace(",", ".")
                val distanceStr = distanceMatch.groupValues[1].replace(",", ".")

                val price = priceStr.toFloat()
                val distance = distanceStr.toFloat()

                if (distance > 0f) {
                    val rate = price / distance

                    val formattedPrice = String.format(Locale("pt", "BR"), "%.2f", price).replace(".", ",")
                    val formattedDistance = String.format(Locale("pt", "BR"), "%.1f", distance).replace(".", ",")
                    val formattedRate = String.format(Locale("pt", "BR"), "%.2f", rate).replace(".", ",")

                    return "Nova corrida. $formattedPrice reais para $formattedDistance quilômetros. Dá $formattedRate reais por quilômetro."
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
