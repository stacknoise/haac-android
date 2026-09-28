package com.stacknoise.haac.app

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stacknoise.haac.app.lock.AppLock
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Application entry point; sets up Hilt and the app lock (concept 5.5). */
@HiltAndroidApp
class HaacApplication : Application() {
    @Inject
    lateinit var appLock: AppLock

    /** Reports foreground and background of the whole app to [AppLock]. */
    override fun onCreate() {
        super.onCreate()
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
