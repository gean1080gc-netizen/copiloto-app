package com.copiloto.auto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import com.copiloto.auto.data.VipFilterRepository
import com.copiloto.auto.data.VoiceShortcuts

class CarNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val title = sbn?.notification?.extras?.getString(Notification.EXTRA_TITLE) ?: return
        val text = sbn.notification?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        val isVip = VipFilterRepository.isContactVip(title)

        if (isVip) {
            // LER EM VOZ ALTA (TTS)
            tts?.speak("Mensagem VIP de $title: $text", TextToSpeech.QUEUE_FLUSH, null, "VIP_MSG")
        } else {
            // BLOQUEAR ÁUDIO & ENVIAR RESPOSTA AUTOMÁTICA
            sendAutoReply(sbn, VoiceShortcuts.NON_VIP_AUTO_REPLY)
        }
    }
}