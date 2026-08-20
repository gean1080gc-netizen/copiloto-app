package com.copiloto.auto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class CarNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        // 📌 Filtrar só WhatsApp
        val pacote = sbn.packageName

        if (pacote != "com.whatsapp") {
            return
        }

        // 📌 Pegar conteúdo da notificação
        val extras = sbn.notification.extras

        val titulo = extras.getString("android.title") ?: "Desconhecido"
        val mensagem = extras.getCharSequence("android.text")?.toString() ?: ""

        // 📌 TESTE (vai aparecer no Logcat)
        android.util.Log.d("COPILOTO", "De: $titulo")
        android.util.Log.d("COPILOTO", "Msg: $mensagem")
    }
}
