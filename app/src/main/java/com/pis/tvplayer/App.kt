package com.pis.tvplayer

import android.app.Application
import com.pis.tvplayer.crash.CrashRecovery
import com.pis.tvplayer.player.MediaCache

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Self-healing kiosk: relaunch the player on any fatal crash.
        CrashRecovery.install(this)
        // Warm the shared media cache early so playback can serve from disk.
        MediaCache.get(this)
    }
}
