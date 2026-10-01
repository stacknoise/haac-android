package com.stacknoise.haac.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stacknoise.haac.app.crash.CrashMarker
import com.stacknoise.haac.app.lock.AppLock
import com.stacknoise.haac.app.shortcut.InstanceShortcuts
import com.stacknoise.haac.core.error.ErrorReporter
import com.stacknoise.haac.core.network.tls.PinSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Application entry point; sets up Hilt, crash reporting (17.4), the app lock (5.5), the certificate pins (4.3)
 * and the instance shortcuts (4.4).
 */
@HiltAndroidApp
class HaacApplication : Application() {
    @Inject
    lateinit var appLock: AppLock

    @Inject
    lateinit var pinSync: PinSync

    @Inject
    lateinit var crashMarker: CrashMarker

    @Inject
    lateinit var errorReporter: ErrorReporter

    @Inject
    lateinit var instanceShortcuts: InstanceShortcuts

    /** Reports foreground and background of the whole app to [AppLock]. */
    override fun onCreate() {
        super.onCreate()
        crashMarker.install()
        crashMarker.replayTo(errorReporter)
        pinSync.start()
        instanceShortcuts.start(ProcessLifecycleOwner.get().lifecycleScope)
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                /** All activities stopped. */
                override fun onStop(owner: LifecycleOwner) = appLock.onBackground()

                /** An activity is visible again. */
                override fun onStart(owner: LifecycleOwner) {
                    owner.lifecycleScope.launch { appLock.onForeground() }
                }
            },
        )
    }
}
