package com.floating.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppPickerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_picker)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerApps)
        val apps = getInstalledApps()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PickerAdapter(apps) { app ->
            val prefs = getSharedPreferences("floating_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_added_package", app.packageName)
                .putString("last_added_name", app.loadLabel(packageManager).toString())
                .apply()
            finish()
        }
    }

    private fun getInstalledApps(): List<ApplicationInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.filter {
            it.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
            it.packageName == "com.zhiliaoapp.musically" || // TikTok
            it.packageName == "com.ss.android.ugc.trill"    // TikTok (региональный)
        }.sortedBy { it.loadLabel(pm).toString() }
    }

    class PickerAdapter(
        private val apps: List<ApplicationInfo>,
        private val onClick: (ApplicationInfo) -> Unit
    ) : RecyclerView.Adapter<PickerAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.pickerIcon)
            val name: TextView = view.findViewById(R.id.pickerName)
            val pkg: TextView = view.findViewById(R.id.pickerPackage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_picker, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            val pm = holder.itemView.context.packageManager
            holder.name.text = app.loadLabel(pm)
            holder.pkg.text = app.packageName
            holder.icon.setImageDrawable(app.loadIcon(pm))
            holder.itemView.setOnClickListener { onClick(app) }
        }

        override fun getItemCount() = apps.size
    }
}
