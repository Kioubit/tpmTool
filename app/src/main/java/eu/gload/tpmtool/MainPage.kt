package eu.gload.tpmtool

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun MainPage(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scroll = rememberScrollState(0)
    Column {
        DevicesDropDownMenu()
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = {viewModel.changePage(Page.ManageDevice)},
                enabled = uiState.selectedDevice != null
            ) {
                Text(text = "Edit Device")
            }
            Button(onClick = { viewModel.addDeviceScreen() }) {
                Text(text = "Add Device")
            }
        }


    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Nonce")
        SelectionContainer {
            Text(
                text = uiState.nonce,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.padding(2.dp))
        QRScanButton()
        EditNonceButton()
    }
}

@Composable
fun QRScanButton(viewModel: MainViewModel = viewModel()) {
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract(),
        onResult = { result ->
            if (result != null) {
                viewModel.attest(result.contents)
            }
        }
    )
    Button(onClick = {
        scanLauncher.launch(
            ScanOptions()
                .setBeepEnabled(false)
                .setBarcodeImageEnabled(false)
                .setTorchEnabled(false)
                .setPrompt("Scan attestation code")
        )
    }) {
        Text(text = "SCAN", fontSize = 30.sp)
    }
}



@Composable
fun EditNonceButton(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var alertDialogShown by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    Button(onClick = {
        alertDialogShown = true
    }, Modifier.padding(0.dp, 3.dp, 0.dp, 10.dp)) {
        Text(text = "Edit nonce")
    }
    if (alertDialogShown) {
        textValue = uiState.nonce
        AlertDialog(onDismissRequest = { alertDialogShown = false }, confirmButton = {
            TextButton(onClick = {
                viewModel.setNonce(textValue.trim())
                alertDialogShown = false
            }
            ) {
                Text(text = "OK")
            }
        }, dismissButton = {
            TextButton(onClick = {
                alertDialogShown = false
            }) {
                Text(text = "Cancel")
            }
        }, text = {
            Column {
                TextField(
                    modifier = Modifier.focusRequester(focusRequester),
                    value = textValue,
                    singleLine = true,
                    label = { Text(text = "Nonce") },
                    onValueChange = { textValue = it })
                Button(onClick = { textValue = "" }) {
                    Text(text = "Clear")
                }
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        })
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesDropDownMenu(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp, 32.dp, 32.dp, 0.dp)
            .wrapContentHeight(Alignment.CenterVertically)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }) {
            TextField(
                value = if (uiState.selectedDevice == null) "No device selected" else uiState.selectedDevice!!.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                label = {
                    Text(
                        text = "Device"
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            // ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Workaround for above having wrong size https://issuetracker.google.com/issues/205589613
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnClickOutside = true,
                    dismissOnBackPress = true
                ),
                modifier = Modifier.exposedDropdownSize()
            ) {
                if (uiState.deviceList != null) {
                    uiState.deviceList!!.forEach {
                        DropdownMenuItem(
                            text = { Text(text = it.name) },
                            onClick = { viewModel.selectDevice(it); expanded = false },
                        )
                    }
                }
            }
        }
    }

}