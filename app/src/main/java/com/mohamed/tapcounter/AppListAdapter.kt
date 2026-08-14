package com.mohamed.tapcounter

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AppEntry(val label: String, val packageName: String, val icon: android.graphics.drawable.Drawable?)

class AppListAdapter(
    private val ctx: Context,
    private val apps: List<AppEntry>
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val cb: CheckBox = view.findViewById(R.id.cbTracked)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val name: TextView = view.findViewById(R.id.tvAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val entry = apps[position]
        holder.name.text = entry.label
        holder.icon.setImageDrawable(entry.icon)

        val tracked = Prefs.getTrackedPackages(ctx)
        holder.cb.setOnCheckedChangeListener(null)
        holder.cb.isChecked = tracked.contains(entry.packageName)
        holder.cb.setOnCheckedChangeListener { _, isChecked ->
            Prefs.setPackageTracked(ctx, entry.packageName, isChecked)
        }
    }

    override fun getItemCount() = apps.size

    companion object {
        fun loadInstalledApps(ctx: Context): List<AppEntry> {
            val pm = ctx.packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            return apps
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map {
                    AppEntry(
                        label = pm.getApplicationLabel(it).toString(),
                        packageName = it.packageName,
                        icon = try { pm.getApplicationIcon(it.packageName) } catch (e: Exception) { null }
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }
}
