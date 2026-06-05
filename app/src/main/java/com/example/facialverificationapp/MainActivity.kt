package com.example.facialverificationapp

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.facialverificationapp.theme.FacialVerificationAppTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Enforce high security by disabling screenshots and screen recordings app-wide
    window.setFlags(
        WindowManager.LayoutParams.FLAG_SECURE,
        WindowManager.LayoutParams.FLAG_SECURE
    )

    enableEdgeToEdge()
    setContent {
      FacialVerificationAppTheme { 
        Surface(
          modifier = Modifier.fillMaxSize(), 
          color = MaterialTheme.colorScheme.background
        ) { 
          MainNavigation() 
        } 
      }
    }
  }
}
