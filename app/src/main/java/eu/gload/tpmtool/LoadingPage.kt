package eu.gload.tpmtool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable


@Composable
fun LoadingPage(){
    Column (verticalArrangement = Arrangement.Center) {
        Text(text = "Please wait...")
    }
}