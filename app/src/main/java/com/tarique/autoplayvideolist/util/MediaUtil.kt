package com.tarique.autoplayvideolist.util

import android.content.Context
import java.util.*

object MediaUtil {

    fun storePlaybackPosition(context: Context, id: Int, position: Long) {
        val sharedPreferenceUtil = SharedPreferenceUtil.getInstance(context)
        val prefKey = SharedPreferenceUtil.LAST_PLAYERD_POSITION.plus("_").plus(id)
        sharedPreferenceUtil.putLong(prefKey, position)
    }

    fun getLastPlayedPositionForVideo(context: Context, id: Int): Long {
        val prefKey = SharedPreferenceUtil.LAST_PLAYERD_POSITION.plus("_").plus(id)
        val sharedPreferenceUtil = SharedPreferenceUtil.getInstance(context)
        return sharedPreferenceUtil.getLong(prefKey, 0)
    }

    fun convertMilliSecondToReadableTimeFormat(duration: Long): String {
        val mFormatBuilder = StringBuilder()
        val mFormatter = Formatter(mFormatBuilder, Locale.getDefault())
        val totalSeconds = duration / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        mFormatBuilder.setLength(0)
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }
}