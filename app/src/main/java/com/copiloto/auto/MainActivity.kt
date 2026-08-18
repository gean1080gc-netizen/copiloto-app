package com.copiloto.auto

import android.Manifest
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkMicrophonePermission()

        tts = TextToSpeech(this, this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        val prefs = getSharedPreferences("copiloto_prefs", Context.MODE_PRIVATE)

        val btnToggleState = findViewById<LinearLayout>(R.id.btnToggleState)
        val txtStatusTitle = findViewById<TextView>(R.id.txtStatusTitle)
        val txtStatusSubtitle = findViewById<TextView>(R.id.txtStatusSubtitle)
        val btnMute = findViewById<LinearLayout>(R.id.btnMute)
        val cardPermissions = findViewById<LinearLayout>(R.id.cardPermissions)
        val btnTestAudio = findViewById<LinearLayout>(R.id.btnTestAudio)

        val chkUber = findViewById<CheckBox>(R.id.chkUber)
        val chk99 = findViewById<CheckBox>(R.id.chk99)
        val chkWhats = findViewById<CheckBox>(R.id.chkWhats)

        var isAssistantActive = prefs.getBoolean("is_active", true)
        chkUber?.isChecked = prefs.getBoolean("chk_uber", true)
        chk99?.isChecked = prefs.getBoolean("chk_99", true)
        chkWhats?.isChecked = prefs.getBoolean("chk_whats", false)

        fun updateUI() {
            if (isAssistantActive) {
                btnToggleState?.setBackgroundColor(Color.parseColor("#00FF66"))
                txtStatusTitle?.text = "ASSISTENTE ATIVO"
                txtStatusTitle?.setTextColor(Color.BLACK)
                txtStatusSubtitle?.text = "Toque para pausar a leitura"
                txtStatusSubtitle?.setTextColor(Color.parseColor("#121212"))
            } else {
                btnToggleState?.setBackgroundColor(Color.parseColor("#222222"))
                txtStatusTitle?.text = "ASSISTENTE PAUSADO"
                txtStatusTitle?.setTextColor(Color.WHITE)
                txtStatusSubtitle?.text = "Toque para reativar"
                txtStatusSubtitle?.setTextColor(Color.parseColor("#888888"))
            }
        }

        updateUI()

        btnToggleState?.setOnClickListener {
            isAssistantActive = !isAssistantActive
            prefs.edit().putBoolean("is_active", isAssistantActive).apply()
            updateUI()
            if (!isAssistantActive) tts?.stop()
            Toast.makeText(this, if (isAssistantActive) "Assistente ativado" else "Assistente pausado", Toast.LENGTH_SHORT).show()
        }

        chkUber?.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("chk_uber", isChecked).apply() }
        chk99?.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("chk_99", isChecked).apply() }
        chkWhats?.setOnCheckedChangeListener { _, isChecked -> prefs.edit().putBoolean("chk_whats", isChecked).apply() }

        btnMute?.setOnClickListener {
            tts?.stop()
            speechRecognizer?.stopListening()
            Toast.makeText(this, "Áudio e escuta interrompidos", Toast.LENGTH_SHORT).show()
        }

        cardPermissions?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnTestAudio?.setOnClickListener {
            if (!isAssistantActive) {
                Toast.makeText(this, "Ative o assistente para ouvir!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startVoiceRecognition()
        }
    }

    private fun checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Toast.makeText(applicationContext, "Fale sua resposta...", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    sendReply(spokenText)
                }
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(applicationContext, "Não foi possível ouvir. Tente novamente.", Toast.LENGTH_SHORT).show()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    private fun sendReply(message: String) {
        val action = MyNotificationService.lastNotificationAction
        if (action == null) {
            Toast.makeText(this, "Nenhuma mensagem recente para responder.", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent()
        val bundle = Bundle()
        for (remoteInput in action.remoteInputs) {
            bundle.putCharSequence(remoteInput.resultKey, message)
        }
        RemoteInput.addResultsToIntent(action.remoteInputs, intent, bundle)

        try {
            action.actionIntent.send(this, 0, intent)
            Toast.makeText(this, "Resposta enviada: $message", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erro ao enviar resposta", Toast.LENGTH_SHORT).show()
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
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
