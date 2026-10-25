package com.example.desafio3dsm

import android.app.Application
import com.google.firebase.database.FirebaseDatabase

class ReadingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching { FirebaseDatabase.getInstance().setPersistenceEnabled(true) }
    }
}
