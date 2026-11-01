package io.github.rool78.java_to_kotlin_flow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import io.github.rool78.java_to_kotlin_flow.ui.StocksRoute
import io.github.rool78.java_to_kotlin_flow.ui.presentation.StocksViewModel
import io.github.rool78.java_to_kotlin_flow.ui.theme.DemoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val stocksViewModel: StocksViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DemoTheme {
                StocksRoute(viewModel = stocksViewModel)
            }
        }
    }
}
