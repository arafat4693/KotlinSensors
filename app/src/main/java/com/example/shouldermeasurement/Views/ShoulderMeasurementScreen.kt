package com.example.shouldermeasurement.Views

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.shouldermeasurement.ViewModels.LinearAccelerationViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.delay

@Composable
fun ShoulderMeasurementScreen(vm: LinearAccelerationViewModel, modifier: Modifier) {
    val algorithm1Angle by vm.algorithm1Angle.collectAsState()
    val algorithm2Angle by vm.algorithm2Angle.collectAsState()

    // Data for the Line Chart
    val algorithm1Entries = remember {mutableListOf<Entry>()}
    val algorithm2Entries = remember {mutableListOf<Entry>()}

    // Track timestamps
    var time by remember { mutableFloatStateOf(0f) }

    var exportPath by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Line Chart
        RealTimeLineChart(
            modifier = Modifier
                .height(300.dp)
                .padding(16.dp)
                .fillMaxWidth(),
            algorithm1Data = algorithm1Entries,
            algorithm2Data = algorithm2Entries
        )

        // Update angles in real-time
        LaunchedEffect(key1 = Unit) { // Trigger only once
            while (true) {
                algorithm1Entries.add(Entry(time, algorithm1Angle))
                algorithm2Entries.add(Entry(time, algorithm2Angle))
                time += 0.05f // Smaller time increment

                // Limit data points
                if (algorithm1Entries.size > 100) {
                    algorithm1Entries.removeAt(0)
                }
                if (algorithm2Entries.size > 100) {
                    algorithm2Entries.removeAt(0)
                }

                delay(20) // Update every 20 milliseconds
            }
        }

        Text(text = "Algorithm 1 (EWMA): ${"%.2f".format(algorithm1Angle)}°")
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Algorithm 2 (Fusion): ${"%.2f".format(algorithm2Angle)}°")
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { vm.startMeasurement() }) {
            Text("Start Measurement")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { vm.stopMeasurement() }) {
            Text("Stop Measurement")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val filePath = vm.exportDataToCsv()
            exportPath = filePath
            if (filePath != null) {
                Toast.makeText(context, "Data exported to: $filePath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to export data", Toast.LENGTH_LONG).show()
            }
        }){
            Text("Export to CSV")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if(exportPath != null) {
            Text(text = "File saved at: $exportPath", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RealTimeLineChart(
    modifier: Modifier = Modifier,
    algorithm1Data: List<Entry>,
    algorithm2Data: List<Entry>
) {
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                setBackgroundColor(Color.White.toArgb())
                description = Description().apply {
                    text = "Angle Measurement"
                    textColor = Color.Black.toArgb()
                }
                xAxis.textColor = Color.Black.toArgb()
                axisLeft.textColor = Color.Black.toArgb()
                axisRight.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(false)
                legend.isEnabled = true
            }
        },
        modifier = modifier
    ) { chart ->
        val dataSets = mutableListOf<ILineDataSet>()
        if(algorithm1Data.isNotEmpty()) {
            val dataSet1 = LineDataSet(algorithm1Data, "Algorithm 1 (EWMA)").apply {
                color = Color.Blue.toArgb()
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
            }
            dataSets.add(dataSet1)
        }

        if(algorithm2Data.isNotEmpty()) {
            val dataSet2 = LineDataSet(algorithm2Data, "Algorithm 2 (Fusion)").apply {
                color = Color.Red.toArgb()
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
            }
            dataSets.add(dataSet2)
        }

        chart.data = LineData(dataSets)
        chart.invalidate() // Refresh the chart
    }
}