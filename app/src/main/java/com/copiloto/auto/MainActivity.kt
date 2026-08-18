package com.copiloto.auto

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        // Botão para simular mensagem em áudio
        val btnSimulate = findViewById<LinearLayout>(R.id.btnSimulate)
        btnSimulate?.setOnClickListener {
            tts?.speak("Nova corrida recebida. Passageiro aguardando a dois minutos do local.", TextToSpeech.QUEUE_FLUSH, null, null)
        }

        // Botão Modo Direção abre a tela de permissões do Android
        val cardDirectionMode = findViewById<LinearLayout>(R.id.cardDirectionMode)
        cardDirectionMode?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            Toast.makeText(this, "Ative o CopilotoAuto na lista", Toast.LENGTH_LONG).show()
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
