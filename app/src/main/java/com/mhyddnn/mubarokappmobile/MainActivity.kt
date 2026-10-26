package com.mhyddnn.mubarokappmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mhyddnn.mubarokappmobile.core.datastore.ThemeMode
import com.mhyddnn.mubarokappmobile.core.designsystem.LoadingView
import com.mhyddnn.mubarokappmobile.core.navigation.AppNavHost
import com.mhyddnn.mubarokappmobile.core.navigation.Routes
import com.mhyddnn.mubarokappmobile.ui.theme.AppTheme
import com.mhyddnn.mubarokappmobile.ui.theme.MubarokAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val loggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }

            MubarokAppTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                    // Tahan di splash sampai tema & sesi terbaca, agar layar pertama tidak berganti warna.
                    when {
                        themeMode == null || loggedIn == null -> LoadingView()
                        loggedIn == true -> AppNavHost(startDestination = Routes.Home)
                        else -> AppNavHost(startDestination = Routes.Login)
                    }
                }
            }
        }
    }
}
