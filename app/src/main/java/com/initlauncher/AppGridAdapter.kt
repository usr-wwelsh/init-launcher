package com.initlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class AppGridAdapter(
    private val apps: MutableList<AppInfo>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<AppGridAdapter.AppViewHolder>() {

    var onLongClick: ((Int) -> Unit)? = null
    var onItemMoved: (() -> Unit)? = null
    var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView as TextView
        var lastTapTime: Long = 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.appName.text = app.name

        // Double-tap to replace app, single tap to launch
        holder.appName.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - holder.lastTapTime < 300) {
                // Double tap detected - show app selector
                onLongClick?.invoke(position)
                holder.lastTapTime = 0  // Reset to prevent triple-tap from being two double-taps
            } else {
                // Single tap - launch app
                onClick(position)
                holder.lastTapTime = currentTime
            }
        }

        // Long press to enable drag
        holder.appName.setOnLongClickListener {
            onStartDrag?.invoke(holder)
            true
        }
    }

    override fun getItemCount(): Int = apps.size

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(apps, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(apps, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
        onItemMoved?.invoke()
    }
}
