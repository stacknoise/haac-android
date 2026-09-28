package com.stacknoise.haac.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; sets up Hilt. */
@HiltAndroidApp
class HaacApplication : Application()
