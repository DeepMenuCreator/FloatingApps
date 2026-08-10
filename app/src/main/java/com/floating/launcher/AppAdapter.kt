package com.floating.launcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppAdapter(
    private val apps: MutableList<AppInfo>,
    private val onResize: (AppInfo, Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onLaunch: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val btnResize: Button = view.findViewById(R.id.btnResize)
        val btnDelete: Button = view.findViewById(R.id.btnDelete)
        val btnLaunch: Button = view.findViewById(R.id.btnLaunch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.appName
        holder.btnResize.setOnClickListener { onResize(app, position) }
        holder.btnDelete.setOnClickListener { onDelete(position) }
        holder.btnLaunch.setOnClickListener { onLaunch(app) }

        try {
            val pm = holder.itemView.context.packageManager
            val info = pm.getApplicationInfo(app.packageName, 0)
            holder.icon.setImageDrawable(info.loadIcon(pm))
        } catch (_: Exception) {
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }

    override fun getItemCount() = apps.size
}
