package com.copiloto.auto

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "Copiloto Auto Ativo!"
            textSize = 22f
            setPadding(60, 60, 60, 60)
        }
        setContentView(textView)
    }
}
