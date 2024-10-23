package eu.gload.tpmtool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ManageDevicePage(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceName = remember{ mutableStateOf(
        uiState.selectedDevice?.name?: ""
    ) }
    val devicePemB64 = remember{ mutableStateOf(
        uiState.selectedDevice?.base64Pem?: ""
    ) }


    Row(Modifier.padding(3.dp)) {
        TextField(value = deviceName.value, onValueChange = {
            deviceName.value = it
        },
            label = { Text("Device Name") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false)
        )
    }
    Row(Modifier.padding(3.dp)) {
        TextField(value = devicePemB64.value,
            onValueChange = {
                devicePemB64.value = it
            },
            label = { Text("BASE64 encoded .pem public Key") },modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false)
        )
    }
    Row(
        modifier = Modifier
            .padding(3.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(onClick = { viewModel.changePage(Page.Main) }) {
            Text(text = "Cancel")
        }

        if (uiState.selectedDevice != null) {
            DeleteDeviceButton()
        }

        Button(onClick = {
            viewModel.addOrEditDevice(deviceName.value,devicePemB64.value)
        }) {
            if (uiState.selectedDevice != null ) {
                Text(text = "Edit device")
            } else {
                Text(text = "Add device")
            }
        }
    }
    Row(Modifier.fillMaxWidth(),horizontalArrangement = Arrangement.Center) {
        if (uiState.selectedDevice != null) {
            Button(onClick = {viewModel.viewLastResult()}, enabled = uiState.selectedDevice!!.attestationJson.isNotEmpty()) {
                Text(text = "View last successful result")
            }
        }
    }
}

@Composable
private fun DeleteDeviceButton(viewModel: MainViewModel = viewModel()) {
    var alertDialogShown by remember { mutableStateOf(false) }
    Button(onClick = {
        alertDialogShown = true
    }) {
        Text(text = "Delete Device")
    }

    if (alertDialogShown) {
        AlertDialog(onDismissRequest = { alertDialogShown = false }, confirmButton = {
            TextButton(onClick = {
                viewModel.deleteSelectedDevice()
                alertDialogShown = false
            }
            ) {
                Text(text = "Yes")
            }
        }, dismissButton = {
            TextButton(onClick = {
                alertDialogShown = false
            }) {
                Text(text = "Cancel")
            }
        }, text = {
            Text(text = "Really delete the selected device?")
        })
    }

}