package com.mohamed.tapcounter

import android.content.Context

object Prefs {
    private const val FILE = "tap_counter_prefs"
    private const val KEY_TRACKED = "tracked_packages"
    private const val KEY_AUTO_ALL_GAMES = "auto_all_games"

    fun getTrackedPackages(ctx: Context): MutableSet<String> {
        val prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_TRACKED, emptySet())!!.toMutableSet()
    }

    fun setPackageTracked(ctx: Context, packageName: String, tracked: Boolean) {
        val prefs = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val current = getTrackedPackages(ctx)
        if (tracked) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_TRACKED, current).apply()
    }

    fun isAutoAllGames(ctx: Context): Boolean {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_ALL_GAMES, true)
    }

    fun setAutoAllGames(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_ALL_GAMES, value).apply()
    }
}
