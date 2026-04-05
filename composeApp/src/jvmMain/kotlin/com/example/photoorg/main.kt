package com.example.photoorg

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javax.swing.UIManager

fun main() = application {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Photo Org",
    ) {
        App()
    }
}