package com.example.shouldermeasurement

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shouldermeasurement.ViewModels.LinearAccelerationViewModel
import com.example.shouldermeasurement.Views.ShoulderMeasurementScreen
import com.example.shouldermeasurement.ui.theme.ShoulderMeasurementTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val linearAccelerationViewModel: LinearAccelerationViewModel = viewModel(
                factory = LinearAccelerationViewModel.Factory
            )

            ShoulderMeasurementTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ShoulderMeasurementScreen(
                        modifier = Modifier.padding(innerPadding),
                        vm = linearAccelerationViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ShoulderMeasurementTheme {
        Greeting("Android")
    }
}