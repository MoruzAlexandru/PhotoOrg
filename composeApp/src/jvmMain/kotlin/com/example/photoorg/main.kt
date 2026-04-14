package com.example.photoorg

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import photoorg.composeapp.generated.resources.Res
import photoorg.composeapp.generated.resources.photo_org_main_icon_color_photo
import javax.swing.UIManager

fun main() = application {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Photo Org",
        icon = painterResource(Res.drawable.photo_org_main_icon_color_photo),
    ) {
        App()
    }
}