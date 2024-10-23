package eu.gload.tpmtool

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.gload.tpmtool.logic.Attestation.AttestationResultType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun AttestationResultPage(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val result = uiState.attestationResult
    if (result == null) {
        Toast.makeText(App.getMContext(),"An error occurred",Toast.LENGTH_SHORT).show()
        return
    }
    var labelBackgroundColor = Color.Red
    var labelText = "N/A"
    var detailsText = "N/A"
    when (result.type) {
        AttestationResultType.OK -> {
            labelBackgroundColor = Color(0xff2bc241)
            labelText = "OK"
            detailsText = result.newAttestationJson
        }

        AttestationResultType.REPLAY -> {
            labelBackgroundColor = Color(0xff2bc241)
            labelText = "OK (Old result)"
            detailsText = result.newAttestationJson
        }

        AttestationResultType.CHANGED -> {
            labelBackgroundColor = Color(0xffffd900)
            labelText = "CHANGED"
            detailsText = result.differencesString
        }

        AttestationResultType.FAILED -> {
            labelBackgroundColor = Color(0xffff4326)
            labelText = "FAILED"
            detailsText = result.failReason
        }

    }

    Column {
        Column (modifier = Modifier.weight(1f)) {
            Text(
                text = labelText, textAlign = TextAlign.Center, modifier = Modifier
                    .background(color = labelBackgroundColor)
                    .fillMaxWidth(), fontSize = 40.sp
            )
            Text(
                text = "Device name: ${result.device?.name}",
                Modifier.padding(6.dp, 2.dp),
                fontSize = 20.sp
            )
            if (result.type == AttestationResultType.REPLAY) {
                val time = result.device?.lastSuccessTime?.let { Date(it * 1000) }
                val timeFormatted =
                    time?.let { SimpleDateFormat("yyyy-MM-dd hh:mma", Locale.US).format(it) }
                Text(text = "Time: $timeFormatted")
            }
            HorizontalDivider(thickness = 1.dp, color = Color.DarkGray)
            val scrollVertical = rememberScrollState(0)
            val scrollHorizontal = rememberScrollState(0)
            SelectionContainer {
                Text(
                    text = detailsText, modifier = Modifier
                        .verticalScroll(scrollVertical)
                        .horizontalScroll(scrollHorizontal)
                )
            }
        }
        Row (horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()){
            Button(onClick = { viewModel.changePage(Page.Main)}, modifier = Modifier
                .padding(1.dp, 0.dp)
                .fillMaxWidth()
                .weight(1f)) {
                Text(text = "Cancel")
            }
            Button(onClick = { viewModel.acceptChanges()}, modifier = Modifier
                .padding(1.dp, 0.dp)
                .fillMaxWidth()
                .weight(1f), enabled = result.type  == AttestationResultType.CHANGED) {
                Text(text = "Accept Changes")
            }
        }
    }

}