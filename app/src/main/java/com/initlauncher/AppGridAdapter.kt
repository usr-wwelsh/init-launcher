package com.initlauncher

import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())
    private val DOUBLE_TAP_DELAY = 300L

    class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appName: TextView = itemView as TextView
        var lastTapTime: Long = 0
        var pendingAction: Runnable? = null
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

            if (currentTime - holder.lastTapTime < DOUBLE_TAP_DELAY) {
                // Double tap detected - cancel pending action and show app selector
                holder.pendingAction?.let { handler.removeCallbacks(it) }
                holder.pendingAction = null
                onLongClick?.invoke(position)
                holder.lastTapTime = 0  // Reset to prevent triple-tap from being two double-taps
            } else {
                // Potential single tap - wait to see if double tap follows
                holder.pendingAction?.let { handler.removeCallbacks(it) }

                val action = Runnable {
                    onClick(position)
                    holder.pendingAction = null
                }

                holder.pendingAction = action
                handler.postDelayed(action, DOUBLE_TAP_DELAY)
                holder.lastTapTime = currentTime
            }
        }

        // Long press to enable drag
        holder.appName.setOnLongClickListener {
            // Cancel any pending tap action when long press is detected
            holder.pendingAction?.let { handler.removeCallbacks(it) }
            holder.pendingAction = null
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
