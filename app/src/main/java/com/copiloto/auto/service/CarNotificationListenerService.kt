package com.copiloto.auto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.util.*

class CarNotificationListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("pt", "BR")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // 🔒 Filtrar só WhatsApp
        val pacote = sbn.packageName
        if (pacote != "com.whatsapp") return

        val extras = sbn.notification.extras

        val titulo = extras.getString("android.title") ?: "Desconhecido"
        val mensagem = extras.getCharSequence("android.text")?.toString() ?: ""

        val textoFalado = "Mensagem de $titulo: $mensagem"

        falar(textoFalado)
    }

    private fun falar(texto: String) {
        tts?.speak(texto, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
