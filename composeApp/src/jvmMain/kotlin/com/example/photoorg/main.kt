package com.example.photoorg

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import photoorg.composeapp.generated.resources.Res
import photoorg.composeapp.generated.resources.photo_org_main_icon_color_photo
import photoorg.composeapp.generated.resources.photo_org_main_icon_white_photo
import kotlin.math.roundToInt
import javax.swing.UIManager

private val WindowWidth = 700.dp
private val BaseWindowHeight = 312.dp
private val TerminalSectionHeight = 320.dp
private val TitleBarHeight = 42.dp

fun main() = application {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    val useDarkTheme = isSystemInDarkTheme()
    val appIcon = if (useDarkTheme) {
        Res.drawable.photo_org_main_icon_white_photo
    } else {
        Res.drawable.photo_org_main_icon_color_photo
    }
    var showTerminalContent by remember { mutableStateOf(false) }
    val windowState = rememberWindowState(
        position = WindowPosition(Alignment.Center),
        size = DpSize(width = WindowWidth, height = BaseWindowHeight + TitleBarHeight),
    )
    val contentHeight = if (showTerminalContent) BaseWindowHeight + TerminalSectionHeight else BaseWindowHeight

    LaunchedEffect(showTerminalContent) {
        windowState.size = DpSize(width = WindowWidth, height = contentHeight + TitleBarHeight)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Photo Org",
        icon = painterResource(appIcon),
        state = windowState,
        undecorated = true,
    ) {
        PhotoOrgTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                PhotoOrgTitleBar(
                    icon = appIcon,
                    onDragWindow = { dx, dy -> window.setLocation(window.x + dx, window.y + dy) },
                    onMinimize = { window.isMinimized = true },
                    onClose = ::exitApplication,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    App(
                        showTerminalContent = showTerminalContent,
                        onToggleTerminalOutput = { showTerminalContent = !showTerminalContent },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoOrgTitleBar(
    icon: DrawableResource,
    onDragWindow: (dx: Int, dy: Int) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val titleBarColor = if (colorScheme.background.red < 0.5f) {
        colorScheme.surface
    } else {
        colorScheme.primary
    }
    val titleColor = if (colorScheme.background.red < 0.5f) {
        colorScheme.onSurface
    } else {
        colorScheme.onPrimary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TitleBarHeight)
            .background(titleBarColor)
            .pointerInput(Unit) {
                @Suppress("DEPRECATION")
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    onDragWindow(dragAmount.x.roundToInt(), dragAmount.y.roundToInt())
                }
            }
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Photo Org",
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinimize, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Minimize",
                    tint = titleColor,
                    modifier = Modifier.width(18.dp),
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = titleColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
