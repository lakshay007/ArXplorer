package com.lakshay.arxplorer

import android.app.Application
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ArXplorerApp : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
    }
} 