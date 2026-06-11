package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.EduHubApp
import com.example.ui.theme.LocalThemeHelper
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeController

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val systemDarkTheme = isSystemInDarkTheme()
      val sharedPreferences = remember {
        getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
      }
      var isDark by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_dark_mode", systemDarkTheme))
      }
      val themeController = remember(isDark) {
        ThemeController(
          isDark = isDark,
          toggleTheme = {
            val newValue = !isDark
            isDark = newValue
            sharedPreferences.edit().putBoolean("is_dark_mode", newValue).apply()
          }
        )
      }

      CompositionLocalProvider(LocalThemeHelper provides themeController) {
        MyApplicationTheme(darkTheme = isDark) {
          Surface(modifier = Modifier.fillMaxSize()) {
            EduHubApp()
          }
        }
      }
    }
  }
}
