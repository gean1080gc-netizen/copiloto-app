package com.copiloto.auto

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("copiloto_prefs", MODE_PRIVATE)

        val chkUber = findViewById<CheckBox>(R.id.chkUber)
        val chk99 = findViewById<CheckBox>(R.id.chk99)
        val chkWhats = findViewById<CheckBox>(R.id.chkWhats)
        val chkIgnoreGroups = findViewById<CheckBox>(R.id.chkIgnoreGroups)
        val btnTestSound = findViewById<Button>(R.id.btnTestSound)

        chkUber.isChecked = prefs.getBoolean("chk_uber", true)
        chk99.isChecked = prefs.getBoolean("chk_99", true)
        chkWhats.isChecked = prefs.getBoolean("chk_whats", false)
        chkIgnoreGroups.isChecked = prefs.getBoolean("chk_ignore_groups", true)

        chkUber.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("chk_uber", isChecked).apply()
        }
        chk99.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("chk_99", isChecked).apply()
        }
        chkWhats.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("chk_whats", isChecked).apply()
        }
        chkIgnoreGroups.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("chk_ignore_groups", isChecked).apply()
        }

        btnTestSound.setOnClickListener {
            val intent = Intent(this, MyNotificationService::class.java)
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationAccess()
    }

    private fun checkNotificationAccess() {
        if (!isNotificationServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Permissão Necessária")
                .setMessage("Para o CoPiloto Auto funcionar, ative a chave do app na próxima tela.")
                .setPositiveButton("Ativar Agora") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
}
