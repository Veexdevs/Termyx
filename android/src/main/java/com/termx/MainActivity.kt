package com.termx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import com.termx.ui.MainScaffold
import com.termx.ui.theme.TermyxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TermyxTheme(darkTheme = isSystemInDarkTheme()) {
                MainScaffold()
            }
        }
    }
}
