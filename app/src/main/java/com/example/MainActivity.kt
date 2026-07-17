package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.SignalConnectRepository
import com.example.ui.*
import com.example.ui.theme.SignalConnectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core persistence setup
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = SignalConnectRepository(
            userDao = database.userDao(),
            routerDao = database.routerDao(),
            usageDao = database.usageDao()
        )

        setContent {
            SignalConnectTheme {
                val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(repository) }
                val dashboardViewModel: DashboardViewModel by viewModels { DashboardViewModelFactory(repository) }

                val authState by authViewModel.uiState.collectAsState()

                when (authState) {
                    is AuthUiState.Authenticated -> {
                        DashboardScreen(
                            authViewModel = authViewModel,
                            dashboardViewModel = dashboardViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    else -> {
                        LoginScreen(
                            authViewModel = authViewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
