package io.github.matheusghenriques.thedoor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

class TimeChangeReceiver : BroadcastReceiver() {
    companion object {
        @Volatile
        var lastTimeChangeAt: Long = 0L
    }

    override fun onReceive(context: Context, intent: Intent) {
        lastTimeChangeAt = SystemClock.elapsedRealtime()
    }
}
