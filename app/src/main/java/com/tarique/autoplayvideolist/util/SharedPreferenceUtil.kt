package com.tarique.autoplayvideolist.util

import android.content.Context

class SharedPreferenceUtil(context: Context) {

    private var sharedPreferences = context.getSharedPreferences("auto-player-pref", Context.MODE_PRIVATE)

    fun putLong(key: String, value: Long) {
        sharedPreferences
            .edit()
            .putLong(key, value)
            .apply()
    }

    fun getLong(key: String, defValue: Long): Long {
        return sharedPreferences.getLong(key, defValue)
    }

    companion object {

        const val SHARED_PREF_NAME = "auto_player_pref"
        const val LAST_PLAYERD_POSITION = "last_played_position"

        private lateinit  var INSTANCE: SharedPreferenceUtil

        fun getInstance(context: Context): SharedPreferenceUtil {
            if (!::INSTANCE.isInitialized) {
                INSTANCE = SharedPreferenceUtil(context)
            }

            return INSTANCE
        }
    }
}