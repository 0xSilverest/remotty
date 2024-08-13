package com.remotty.clientgui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*

@Composable
fun ConnectionScreen(
    onConnect: (String) -> Unit,
    onScan: (onScanComplete: (String?) -> Unit) -> Unit
) {
    var ipAddress by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            TextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("Enter IP address") },
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = { onConnect(ipAddress) },
                    enabled = !isLoading && ipAddress.isNotEmpty() && ipAddress.isNotBlank()
                ) {
                    Text("Connect")
                }

                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            scanResult = null
                            onScan { result ->
                                isLoading = false
                                scanResult = result
                                if (result != null) {
                                    ipAddress = result
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("Scan for Servers")
                }
            }

            if (scanResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Server found at: $scanResult")
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onConnect(scanResult!!) },
                    enabled = !isLoading
                ) {
                    Text("Connect to Found Server")
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Scanning for servers", color = Color.White)
                }
            }
        }
    }
}