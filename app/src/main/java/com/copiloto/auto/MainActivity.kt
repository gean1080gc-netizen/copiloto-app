package com.copiloto.auto

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Criando botão na tela para abrir permissões direto, sem depender de XML
        val button = Button(this).apply {
            text = "Ativar Permissão de Notificação"
            textSize = 18f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        setContentView(button)
    }
}
