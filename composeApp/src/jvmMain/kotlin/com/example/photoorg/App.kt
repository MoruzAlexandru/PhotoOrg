package com.example.photoorg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.painterResource

import java.io.File
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.UIManager

import photoorg.composeapp.generated.resources.Res
import photoorg.composeapp.generated.resources.compose_multiplatform

private val PhotoOrgColorScheme = lightColorScheme(
    primary = Color(0xFF2F5D62),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5E6B73),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F5F7),
    onBackground = Color(0xFF1B1F23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F23),
)

private fun pickFolder(initialPath: String): String? {
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        return pickWindowsFolder(initialPath)
    }

    return pickSwingFolder(initialPath)
}

private fun pickWindowsFolder(initialPath: String): String? {
    val escapedPath = initialPath.replace("'", "''")
    val script = """
Add-Type -AssemblyName System.Windows.Forms
${'$'}dialog = New-Object System.Windows.Forms.OpenFileDialog
${'$'}dialog.Title = 'Select Folder'
${'$'}dialog.ValidateNames = ${'$'}false
${'$'}dialog.CheckFileExists = ${'$'}false
${'$'}dialog.CheckPathExists = ${'$'}true
${'$'}dialog.FileName = 'Select this folder'
if ('$escapedPath' -ne '') {
    ${'$'}dialog.InitialDirectory = '$escapedPath'
}
if (${'$'}dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    Write-Output (Split-Path ${'$'}dialog.FileName -Parent)
}
""".trimIndent()

    return runCatching {
        val process = ProcessBuilder(
            "powershell",
            "-NoProfile",
            "-STA",
            "-Command",
            script
        ).start()

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).readText().trim()
        process.waitFor()

        output.ifBlank { null }
    }.getOrNull()
}

private fun pickSwingFolder(initialPath: String): String? {
    runCatching {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }

    val chooser = JFileChooser().apply {
        dialogTitle = "Select Folder"
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isAcceptAllFileFilterUsed = false
        if (initialPath.isNotBlank()) {
            currentDirectory = File(initialPath)
        }
    }

    return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile?.absolutePath
    } else {
        null
    }
}

//$env:JAVA_HOME = "C:\Users\moruz\AppData\Local\Programs\IntelliJ IDEA Community Edition\jbr"
//$env:Path = "$env:JAVA_HOME\bin;$env:Path"
//java -version
//.\gradlew.bat :composeApp:run

@Composable
@Preview
fun App() {
    MaterialTheme(colorScheme = PhotoOrgColorScheme) {
        var albumNameOrPathCheck by remember { mutableStateOf(true) }
        var albumName by remember { mutableStateOf("") }
        var albumPathFinal by remember { mutableStateOf("D:\\Poze Canon\\") }

        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = albumNameOrPathCheck,
                    onCheckedChange = {albumNameOrPathCheck = it}
                )
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text("New Album's Name") },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    enabled = albumNameOrPathCheck
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Checkbox Field
                Checkbox(
                    checked = !albumNameOrPathCheck,
                    onCheckedChange = {
                        if (albumNameOrPathCheck) {albumNameOrPathCheck = false} else {albumNameOrPathCheck = true};
                    }
                )
                // 2. The Path Field
                OutlinedTextField(
                    value = albumPathFinal,
                    onValueChange = { albumPathFinal = it },
                    label = { Text("Selected Path") },
                    modifier = Modifier.fillMaxWidth(0.59f),
                    readOnly = true, // Prevents manual typing if preferred
                    enabled = !albumNameOrPathCheck,
                )

                // 3. Added a space of 8 dp between the path text field and Open button
                Spacer(modifier = Modifier.width(8.dp))

                // 4. The Explorer Button
                Button(
                    enabled = !albumNameOrPathCheck,
                    onClick = {
                        pickFolder(albumPathFinal)?.let { selectedPath ->
                            albumPathFinal = selectedPath
                        }
                    }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Open Explorer")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = { showContent = !showContent }) {
                    Text("Click me!")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("New Row")
            }
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Album Name: $albumName")
            }
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Album Path: $albumPathFinal")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}
