package com.pis.tvplayer.crash

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.pis.tvplayer.ui.PlayerActivity
import kotlin.system.exitProcess

/**
 * Installs a global uncaught-exception handler that schedules an immediate relaunch of
 * the player via AlarmManager, then terminates the crashed process. This keeps the kiosk
 * self-healing: any fatal error results in the player coming back up a second later.
 */
class CrashRecovery private constructor(
    private val context: Context,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "Uncaught exception — scheduling restart", throwable)
        try {
            scheduleRestart()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to schedule restart", t)
        } finally {
            // Let any chained handler (e.g. crash reporter) run, then hard-exit.
            previous?.uncaughtException(thread, throwable)
            exitProcess(2)
        }
    }

    private fun scheduleRestart() {
        val intent = Intent(context, PlayerActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val flags = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, RESTART_REQUEST, intent, flags)

        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + RESTART_DELAY_MS,
            pending
        )
    }

    companion object {
        private const val TAG = "CrashRecovery"
        private const val RESTART_REQUEST = 4711
        private const val RESTART_DELAY_MS = 1_000L

        fun install(context: Context) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            if (previous is CrashRecovery) return
            Thread.setDefaultUncaughtExceptionHandler(
                CrashRecovery(context.applicationContext, previous)
            )
        }
    }
}
