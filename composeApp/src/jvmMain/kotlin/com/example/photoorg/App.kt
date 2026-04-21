package com.example.photoorg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.UIManager

import photoorg.composeapp.generated.resources.Res
import photoorg.composeapp.generated.resources.compose_multiplatform
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


var defaultPathDestination = "D:\\Poze Canon\\"
val jpegDirectoryName = "JPEG"
val rawDirectoryName = "RAW"
val jpegFormats = listOf("jpg", "jpeg")
val rawFormats = listOf("cr3")

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

private fun getFilesList(sourcePath: String): List<File> {
    val sourceDir = File(sourcePath)
    if (!sourceDir.exists() || !sourceDir.isDirectory) {
        println("Source path '$sourcePath' is not a valid directory.")
        return emptyList()
    }

    return sourceDir.listFiles()?.filter { it.isFile } ?: emptyList()
}

private fun copyFiles(files: List<File>, destinationPath: File) {
    files.forEach { file ->
        val fileExtension = file.extension.lowercase()

        var extensionDir: File? = null
        if (fileExtension in jpegFormats) {
            extensionDir = File(destinationPath, jpegDirectoryName)
        }
        if (fileExtension in rawFormats) {
            extensionDir = File(destinationPath, rawDirectoryName)
        }
        if (extensionDir != null) {
            val destFile = File(extensionDir, file.name)
            Files.copy(file.toPath(), destFile.toPath())
            println("Copied '${file.absolutePath}' to '${destFile.absolutePath}'")
        }
    }
}

private fun moveFiles(files: List<File>, destinationPath: File) {
    files.forEach { file ->
        val fileExtension = file.extension.lowercase()

        var extensionDir: File? = null
        if (fileExtension in jpegFormats) {
            extensionDir = File(destinationPath, jpegDirectoryName)
        }
        if (fileExtension in rawFormats) {
            extensionDir = File(destinationPath, rawDirectoryName)
        }
        if (extensionDir != null) {
            val destFile = File(extensionDir, file.name)
            Files.move(file.toPath(), destFile.toPath())
            println("Moved '${file.absolutePath}' to '${destFile.absolutePath}'")
        }
    }
}

private fun processFiles(sourcePath: String, destinationPathIn: String, deleteFiles: Boolean = false): Boolean {
    if (destinationPathIn == defaultPathDestination) {
        println("Destination path is still the default path '$defaultPathDestination'")
        return true
    }

    var destinationPath = destinationPathIn
    if ('/' !in destinationPath && '\\' !in destinationPath) {
        destinationPath = "$defaultPathDestination\"$destinationPath"
    }

    println("Processing files from '$sourcePath' to '$destinationPath'. Files are ${if (deleteFiles) "being moved" else "being copied"}.")

    // check source directory exists
    val sourceDir = File(sourcePath).isDirectory
    if (!sourceDir) {
        println("Source path '$sourcePath' is not a valid directory")
        return false
    }

    // check destination directory exists and create it if not
    var destinationDir = File(destinationPath)
    if (!destinationDir.exists() || !destinationDir.isDirectory){
        println("Destination path '$destinationPath' is not a valid directory")
        println("Album '$destinationPath' will be created inside default directory '$defaultPathDestination'")
        destinationDir = File(defaultPathDestination)
    }
    // create RAW and JPEG folders inside destination directory
    val createdRaw = File(destinationDir, "RAW").mkdirs()
    println("Created: $createdRaw")
    val createdJpeg = File(destinationDir, "JPEG").mkdirs()
    println("Created: $createdJpeg")

    // extract file path list from source directory
    val files_list = getFilesList(sourcePath)
    if (files_list.isEmpty()) {
        println("No files found in source directory '$sourcePath'")
        return false
    }
    println(files_list)
    // copy files to destination
    if (!deleteFiles){
        println("Files are copied to '$destinationDir'")
        copyFiles(files_list, destinationDir)
    }
    // move files from source
    if (deleteFiles){
        println("Files are moved to '$destinationDir'")
        moveFiles(files_list, destinationDir)
    }

    return false
}

@Composable
@Preview
fun App() {
    var albumName by remember { mutableStateOf("") }
    var albumPathSource by remember {mutableStateOf("D:\\Poze Canon\\106CANON")}

    var albumPathDestination by remember { mutableStateOf(defaultPathDestination) }
    var destinationBlinkTrigger by remember { mutableStateOf(0) }
    var highlightDestinationPath by remember { mutableStateOf(false) }

    LaunchedEffect(destinationBlinkTrigger) {
        if (destinationBlinkTrigger == 0) {
            return@LaunchedEffect
        }

        repeat(5) {
            highlightDestinationPath = true
            delay(180.milliseconds)
            highlightDestinationPath = false
            delay(180.milliseconds)
        }
    }

    MaterialTheme(colorScheme = PhotoOrgColorScheme) {
        val destinationPromptColor by animateColorAsState(
            targetValue = if (highlightDestinationPath) Color(0xFFD97706) else MaterialTheme.colorScheme.primary,
            label = "destinationPromptColor"
        )

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
                // 1. The Path Field
                OutlinedTextField(
                    value = albumPathDestination,
                    onValueChange = { albumPathDestination = it },
                    label = { Text("Destination Album Path") },
                    modifier = Modifier.fillMaxWidth(0.59f),
                    readOnly = false, // Prevents manual typing if preferred
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = destinationPromptColor,
                        unfocusedBorderColor = destinationPromptColor,
                        focusedLabelColor = destinationPromptColor,
                        unfocusedLabelColor = destinationPromptColor,
                        focusedSupportingTextColor = destinationPromptColor,
                        unfocusedSupportingTextColor = destinationPromptColor,
                    )
                )

                // 2. Added a space of 8 dp between the path text field and Open button
                Spacer(modifier = Modifier.width(8.dp))

                // 3. The Explorer Button
                Button(
                    onClick = {
                        pickFolder(albumPathDestination)?.let { selectedPath ->
                            albumPathDestination = selectedPath
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
                Text("Album Path: $albumPathDestination")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Copy photos Button
                Button(
                    onClick = {
                        val shouldBlink = processFiles(albumPathSource, albumPathDestination)
                        if (shouldBlink) {
                            destinationBlinkTrigger++
                        }
                    },
                    ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Photos")
                    Text(" Copy photos")
                }

                // 2. Added a space of 20 dp between the 2 buttons
                Spacer(modifier = Modifier.width(20.dp))

                // 3. Move and delete photos Button
                Button(
                    onClick = {
                        val shouldBlink = processFiles(albumPathSource, albumPathDestination, deleteFiles=true)
                        if (shouldBlink) {
                            destinationBlinkTrigger++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB0B0B0),
                        contentColor = Color(0xFF4F4F4F),
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Move Photos")
                    Text(" Move photos")
                }
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
