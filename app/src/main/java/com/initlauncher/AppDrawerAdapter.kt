package com.initlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.initlauncher.theme.ThemeApplier
import com.initlauncher.theme.ThemeManager

class AppDrawerAdapter(
    private val apps: List<AppInfo>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.AppViewHolder>() {

    var onLongClick: ((Int) -> Unit)? = null

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
        val appName: TextView = itemView.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_drawer, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name
        holder.appIcon.setImageDrawable(app.icon)
        ThemeApplier.apply(holder.itemView, ThemeManager.getActiveTheme(holder.itemView.context))

        holder.itemView.setOnClickListener {
            onClick(position)
        }

        holder.itemView.setOnLongClickListener {
            onLongClick?.invoke(position)
            true
        }
    }

    override fun getItemCount(): Int = apps.size
}
