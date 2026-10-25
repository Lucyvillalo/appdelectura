package com.example.desafio3dsm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.desafio3dsm.ui.ReadingApp
import com.example.desafio3dsm.viewmodel.ReadingViewModel
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF385D4B), secondary = Color(0xFF8B624B), background = Color(0xFFF8F6F0))) {
                val vm: ReadingViewModel = viewModel()
                ReadingApp(vm)
            }
        }
    }
}
