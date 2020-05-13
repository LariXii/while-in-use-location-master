package com.example.android.whileinuselocation

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.edit


@SuppressLint("Registered")
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            handleUncaughtException(thread, e)
        }
    }

    private fun handleUncaughtException(thread: Thread, e: Throwable) {
            applicationContext.getSharedPreferences(
                applicationContext.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE).edit {
                putBoolean(SharedPreferenceUtil.KEY_APPLICATION_CRASHED, true)
            }
        Log.d("Application", "L'application à crashé !!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }
}