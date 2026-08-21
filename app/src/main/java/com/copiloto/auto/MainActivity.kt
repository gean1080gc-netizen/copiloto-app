package com.copiloto.auto

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.TextView
import java.util.*

class MainActivity : Activity() {

    private val REQ_VOICE = 100

    lateinit var texto: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        texto = TextView(this)
        val botao = Button(this)

        texto.text = "Fale: 'Responder algo'"

        botao.text = "🎤 Falar"

        botao.setOnClickListener {
            ouvirVoz()
        }

        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.addView(texto)
        layout.addView(botao)

        setContentView(layout)
    }

    private fun ouvirVoz() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("pt", "BR"))

        startActivityForResult(intent, REQ_VOICE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQ_VOICE && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoFalado = result?.get(0) ?: ""

            texto.text = "Você falou: $textoFalado"

            processarComando(textoFalado)
        }
    }

    private fun processarComando(texto: String) {

        if (texto.lowercase().startsWith("responder")) {
            val resposta = texto.replace("responder", "").trim()

            texto = "Resposta pronta: $resposta"
        }
    }
}
