package eu.gload.tpmtool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddDevicePage(viewModel: MainViewModel = viewModel()) {
    var deviceName by remember{ mutableStateOf("") }
    var devicePemB64 by remember{ mutableStateOf("") }
    Row(Modifier.padding(3.dp)) {
        TextField(value = deviceName, onValueChange = {
            deviceName = it
        },
            label = { Text("Device Name") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrect = false)
        )
    }
    Row(Modifier.padding(3.dp)) {
        TextField(value = devicePemB64,
            onValueChange = {
                devicePemB64 = it
            },
            label = { Text("BASE64 encoded .pem public Key") },modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrect = false)

        )
    }
    Row(
        modifier = Modifier
            .padding(3.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Button(onClick = { viewModel.changePage(Pages.Main) }) {
            Text(text = "Abort")
        }

        Button(onClick = {
            viewModel.addDevice(deviceName,devicePemB64)
        }) {
            Text(text = "Add device")
        }

    }
}