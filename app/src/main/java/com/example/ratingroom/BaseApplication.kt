package com.example.ratingroom

import android.R
import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication: Application() {
    override fun onCreate() {
        val Pruebas: Boolean = false
        super.onCreate()
        if(Pruebas == true){
            if (BuildConfig.DEBUG) {
                Firebase.firestore.useEmulator("10.0.2.2", 8080)
                Firebase.auth.useEmulator("10.0.2.2", 9099)
            }
        }
    }
}