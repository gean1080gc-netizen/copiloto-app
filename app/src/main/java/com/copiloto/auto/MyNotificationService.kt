package com.copiloto.auto

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.Locale

class MyNotificationService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
            tts?.setPitch(0.9f)       // Tom um pouco mais grave
            tts?.setSpeechRate(0.9f)  // Velocidade mais cadenciada
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

        // 1. FILTRO: Ignorar conversas de grupo (se a opção estiver ativa)
        val isGroup = extras.getBoolean("android.isGroupConversation", false)
        val ignoreGroups = prefs.getBoolean("chk_ignore_groups", true)
        if (ignoreGroups && isGroup) return

        val title = extras.getString("android.title") ?: ""
        var text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (text.isEmpty()) return

        // 2. FILTRO: Ignorar Códigos de Confirmação / SMS / PIN
        val hasCodeWord = text.contains("código", ignoreCase = true) ||
                          text.contains("pin", ignoreCase = true) ||
                          text.contains("verificação", ignoreCase = true)
        val hasNumbersOnly = text.contains(Regex("\\b\\d{4,6}\\b")) // Procura 4 a 6 dígitos numéricos isolados

        if (hasCodeWord || hasNumbersOnly) return

        // Mapeamento dos aplicativos autorizados
        val checkUber = prefs.getBoolean("chk_uber", true)
        val check99 = prefs.getBoolean("chk_99", true)
        val checkWhats = prefs.getBoolean("chk_whats", false)

        val isUber = packageName.contains("ubercab") && checkUber
        val is99 = packageName.contains("taxis99") && check99
        val isWhats = packageName.contains("whatsapp") && checkWhats

        if (isUber || is99 || isWhats) {

            // 3. LIMITE DE TAMANHO: Truncar textos muito longos
            if (text.length > 100) {
                text = text.take(100) + "... mensagem longa."
            }

            // 4. BIPE SONORO: Toca um 'pip' antes de falar
            playBeepSound()

            val fullMessage = "$title disse: $text"
            tts?.speak(fullMessage, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    // Função responsável pelo som de confirmação
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
