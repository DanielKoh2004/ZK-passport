package com.example.zk.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.zk.ui.theme.ZKTheme

@Composable
fun MyQrScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("My QR")
    }
}

@Preview(showBackground = true)
@Composable
fun MyQrScreenPreview() {
    ZKTheme {
        MyQrScreen()
    }
}
