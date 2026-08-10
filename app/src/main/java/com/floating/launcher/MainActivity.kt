package com.floating.launcher

import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var switchEnable: Switch
    private lateinit var btnFreeformSettings: Button

    private val apps = mutableListOf<AppInfo>()
    private lateinit var adapter: AppAdapter
    private val prefs by lazy { getSharedPreferences("floating_prefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        switchEnable = findViewById(R.id.switchEnable)
        btnFreeformSettings = findViewById(R.id.btnFreeformSettings)

        loadApps()

        adapter = AppAdapter(apps,
            onResize = { app, pos -> showResizeDialog(app, pos) },
            onDelete = { pos -> deleteApp(pos) },
            onLaunch = { app -> launchAppFreeform(app) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        switchEnable.isChecked = prefs.getBoolean("enabled", false)
        updateFabVisibility()

        switchEnable.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enabled", isChecked).apply()
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    requestOverlayPermission()
                    switchEnable.isChecked = false
                    return@setOnCheckedChangeListener
                }
                startFloatingService()
            } else {
                stopFloatingService()
            }
            updateFabVisibility()
        }

        fabAdd.setOnClickListener {
            startActivity(Intent(this, AppPickerActivity::class.java))
        }

        btnFreeformSettings.setOnClickListener {
            openFreeformSettings()
        }
    }

    private fun updateFabVisibility() {
        fabAdd.visibility = if (switchEnable.isChecked) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun requestOverlayPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivity(intent)
    }

    private fun startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, FloatingService::class.java))
        } else {
            startService(Intent(this, FloatingService::class.java))
        }
    }

    private fun stopFloatingService() {
        stopService(Intent(this, FloatingService::class.java))
    }

    private fun launchAppFreeform(app: AppInfo) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent == null) {
                Toast.makeText(this, "Не удалось запустить приложение", Toast.LENGTH_SHORT).show()
                return
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)

            val bounds = Rect(app.x, app.y, app.x + app.width, app.y + app.height)

            val options = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ActivityOptions.makeBasic().setLaunchBounds(bounds)
            } else {
                ActivityOptions.makeBasic()
            }

            // Freeform через reflection для старых версий
            try {
                val method = ActivityOptions::class.java.getMethod("setLaunchWindowingMode", Int::class.java)
                method.invoke(options, 5) // 5 = WINDOWING_MODE_FREEFORM
            } catch (_: Exception) {
                // Не поддерживается на этом устройстве
            }

            startActivity(intent, options.toBundle())
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResizeDialog(app: AppInfo, position: Int) {
        val dialog = ResizeDialog(this, app) { newWidth, newHeight ->
            app.width = newWidth
            app.height = newHeight
            saveApps()
            adapter.notifyItemChanged(position)
        }
        dialog.show()
    }

    private fun deleteApp(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить?")
            .setMessage("Убрать ${apps[position].appName} из списка?")
            .setPositiveButton("Удалить") { _, _ ->
                apps.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveApps()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun openFreeformSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Включи 'Force activities to be resizable'", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось открыть настройки разработчика", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveApps() {
        val json = JSONArray()
        apps.forEach { app ->
            val obj = JSONObject()
            obj.put("packageName", app.packageName)
            obj.put("appName", app.appName)
            obj.put("width", app.width)
            obj.put("height", app.height)
            obj.put("x", app.x)
            obj.put("y", app.y)
            json.put(obj)
        }
        prefs.edit().putString("apps", json.toString()).apply()
    }

    private fun loadApps() {
        apps.clear()
        val jsonStr = prefs.getString("apps", "[]") ?: "[]"
        val json = JSONArray(jsonStr)
        for (i in 0 until json.length()) {
            val obj = json.getJSONObject(i)
            apps.add(AppInfo(
                obj.getString("packageName"),
                obj.getString("appName"),
                obj.getInt("width"),
                obj.getInt("height"),
                obj.getInt("x"),
                obj.getInt("y")
            ))
        }
    }

    override fun onResume() {
        super.onResume()
        // Проверяем, не добавилось ли приложение из AppPicker
        val newPkg = prefs.getString("last_added_package", null)
        val newName = prefs.getString("last_added_name", null)
        if (newPkg != null && newName != null) {
            prefs.edit().remove("last_added_package").remove("last_added_name").apply()
            apps.add(AppInfo(newPkg, newName))
            saveApps()
            adapter.notifyItemInserted(apps.size - 1)
        }
    }
}
