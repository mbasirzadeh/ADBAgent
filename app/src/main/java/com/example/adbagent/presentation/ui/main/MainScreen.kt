package com.example.adbagent.presentation.ui.main

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.adbagent.presentation.model.UiEvent
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = koinViewModel()) {
    val rsaState = vm.rsa.collectAsState()
    var rsaInput by remember { mutableStateOf(rsaState.value) }
    val portState = vm.port.collectAsState()
    var portInput by remember { mutableStateOf(portState.value.toString()) }
    val endpoint = vm.endpoint.collectAsState("")

    LaunchedEffect(rsaState.value) { rsaInput = rsaState.value }
    LaunchedEffect(portState.value) { portInput = portState.value.toString() }

    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        // Collect UI events
        vm.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> Toast.makeText(ctx, event.msg, Toast.LENGTH_LONG).show()
                is UiEvent.CopyText -> clipboard.setText(AnnotatedString(event.text))
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("ADB Agent") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = rsaInput,
                onValueChange = { rsaInput = it },
                label = { Text("RSA Public Key") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Button(
                onClick = { vm.saveRSAKey(rsaInput) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("save RSA key") }
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it.filter { c -> c.isDigit() } },
                label = { Text("ADB Port") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(
                onClick = {
                    vm.savePort(portInput.toIntOrNull())
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("save port") }

            Button(
                onClick = { vm.startAdb(portInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 25.dp)
            ) { Text("Start ADB") }
            if (endpoint.value.isNotBlank()) Text("Current: ${endpoint.value}")
        }
    }
}