package com.copiloto.auto

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isAssistantActive = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        val btnToggleState = findViewById<LinearLayout>(R.id.btnToggleState)
        val txtStatusTitle = findViewById<TextView>(R.id.txtStatusTitle)
        val txtStatusSubtitle = findViewById<TextView>(R.id.txtStatusSubtitle)
        val btnMute = findViewById<LinearLayout>(R.id.btnMute)
        val cardPermissions = findViewById<LinearLayout>(R.id.cardPermissions)
        val btnTestAudio = findViewById<LinearLayout>(R.id.btnTestAudio)

        // Alternar Estado Ativo / Pausado
        btnToggleState?.setOnClickListener {
            isAssistantActive = !isAssistantActive

            if (isAssistantActive) {
                btnToggleState.setBackgroundColor(Color.parseColor("#00FF66"))
                txtStatusTitle?.text = "ASSISTENTE ATIVO"
                txtStatusTitle?.setTextColor(Color.BLACK)
                txtStatusSubtitle?.text = "Toque para pausar a leitura"
                txtStatusSubtitle?.setTextColor(Color.parseColor("#121212"))
                Toast.makeText(this, "Assistente ativado", Toast.LENGTH_SHORT).show()
            } else {
                btnToggleState.setBackgroundColor(Color.parseColor("#222222"))
                txtStatusTitle?.text = "ASSISTENTE PAUSADO"
                txtStatusTitle?.setTextColor(Color.WHITE)
                txtStatusSubtitle?.text = "Toque para reativar"
                txtStatusSubtitle?.setTextColor(Color.parseColor("#888888"))
                tts?.stop()
                Toast.makeText(this, "Assistente pausado", Toast.LENGTH_SHORT).show()
            }
        }

        // Silenciar áudio imediatamente
        btnMute?.setOnClickListener {
            tts?.stop()
            Toast.makeText(this, "Áudio interrompido", Toast.LENGTH_SHORT).show()
        }

        // Abrir configurações de notificação
        cardPermissions?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Testar leitura de voz
        btnTestAudio?.setOnClickListener {
            if (!isAssistantActive) {
                Toast.makeText(this, "Ative o assistente para ouvir!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val textToSpeak = "Nova corrida recebida na 99: Passageiro a 800 metros do local de embarque."
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
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
