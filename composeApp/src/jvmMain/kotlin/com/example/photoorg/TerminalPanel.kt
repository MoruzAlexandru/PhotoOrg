package com.example.photoorg

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TerminalPanel(
    modifier: Modifier = Modifier,
    title: String,
    lines: List<String>,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkTheme = colorScheme.background.red < 0.5f
    val outerBackground = if (isDarkTheme) Color(0xFF2A2D31) else Color(0xFFF8FAFC)
    val outerBorder = if (isDarkTheme) Color(0xFF40444A) else Color(0xFFD8E0EA)
    val titleColor = if (isDarkTheme) Color(0xFFD3D7DC) else Color(0xFF5B6B7F)
    val innerBackground = if (isDarkTheme) Color(0xFF1F2226) else Color(0xFFFFFFFF)
    val promptColor = if (isDarkTheme) Color(0xFFA6D6A0) else Color(0xFF2F6F44)
    val lineColor = if (isDarkTheme) Color(0xFFE7E9EC) else Color(0xFF233142)

    Column(
        modifier = modifier
            .background(outerBackground)
            .border(1.dp, outerBorder)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("o", color = Color(0xFFFF5F56))
            Spacer(modifier = Modifier.width(6.dp))
            Text("o", color = Color(0xFFFFBD2E))
            Spacer(modifier = Modifier.width(6.dp))
            Text("o", color = Color(0xFF27C93F))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = titleColor,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(innerBackground)
                .padding(12.dp),
        ) {
            if (lines.isEmpty()) {
                Text(
                    text = "> waiting for output...",
                    color = promptColor,
                    fontFamily = FontFamily.Monospace,
                )
            } else {
                LazyColumn {
                    items(lines) { line ->
                        Text(
                            text = "> $line",
                            color = lineColor,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
