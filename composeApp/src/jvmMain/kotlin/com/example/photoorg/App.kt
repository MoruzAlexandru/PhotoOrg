package com.example.photoorg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.UIManager

import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds


var defaultPathDestination = "D:\\Poze Canon\\"
val jpegDirectoryName = "JPEG"
val rawDirectoryName = "RAW"
val jpegFormats = listOf("jpg", "jpeg")
val rawFormats = listOf("cr3")
val blinkColor = Color(0xFFD97706)

private val PhotoOrgColorSchemeLight = lightColorScheme(
    primary = Color(0xFF2F5D62),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF5E6B73),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F5F7),
    onBackground = Color(0xFF1B1F23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1F23),
)
private val PhotoOrgColorSchemeDark = darkColorScheme(
    primary = Color(0xFFB6BDC6),
    onPrimary = Color(0xFF161A1E),
    secondary = Color(0xFF8F98A3),
    onSecondary = Color(0xFF13171B),
    background = Color(0xFF1A1C1F),
    onBackground = Color(0xFFE7E9EC),
    surface = Color(0xFF26292D),
    onSurface = Color(0xFFE7E9EC),
)

@Composable
fun PhotoOrgTheme(content: @Composable () -> Unit) {
    val useDarkTheme = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (useDarkTheme) PhotoOrgColorSchemeDark else PhotoOrgColorSchemeLight,
        content = content,
    )
}

private val BLINK_SOURCE_PATH = "blink_source_path"
private val BLINK_DEST_PATH = "blink_dest_path"
private val NO_FILES_FOUND = "no_files_found"
private val NO_ERROR = "no_error"

private object AppConsole {
    private const val maxLines = 200
    private val _lines = mutableListOf<String>().toMutableStateList()
    val lines: SnapshotStateList<String> = _lines

    fun log(message: String) {
        println(message)
        _lines += message
        if (_lines.size > maxLines) {
            _lines.removeRange(0, _lines.size - maxLines)
        }
    }
}

private fun appLog(message: String) = AppConsole.log(message)

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
        appLog("Source path '$sourcePath' is not a valid directory.")
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
//            Files.copy(file.toPath(), destFile.toPath())
            Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            appLog("Copied '${file.absolutePath}' to '${destFile.absolutePath}'")
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
//            Files.move(file.toPath(), destFile.toPath())
            Files.move(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            appLog("Moved '${file.absolutePath}' to '${destFile.absolutePath}'")
        }
    }
}

private fun processFiles(sourcePathIn: String, destinationPathIn: String, deleteFiles: Boolean = false): String {
    var destinationPath = destinationPathIn
    var sourcePath = sourcePathIn

    if (destinationPathIn == defaultPathDestination) {
        appLog("Destination path is still the default path -> '$defaultPathDestination'")
        return BLINK_DEST_PATH
    }

    if ('/' !in destinationPath && '\\' !in destinationPath) {
        destinationPath = "$defaultPathDestination\"$destinationPath"
    }

    if (sourcePath.isEmpty()) {
        appLog("Source path is destination path -> '$defaultPathDestination'")
        sourcePath = destinationPath
    }

    appLog("Processing files from -> '$sourcePath'.")
    appLog("Processing files to   -> '$destinationPath'.")
    appLog("Files are ${if (deleteFiles) "being moved" else "being copied"}.")

    // check destination directory exists and create it if not
    var destinationDir = File(destinationPath)
    if (!destinationDir.exists() || !destinationDir.isDirectory){
        appLog("Destination path '$destinationPath' is not a valid directory")
        appLog("Album '$destinationPath' will be created inside default directory '$defaultPathDestination'")
        destinationDir = File(defaultPathDestination)
    }

    // check source directory exists
    val sourceFile = File(sourcePath)
    if (!sourceFile.exists() || !sourceFile.isDirectory) {
        appLog("Source path '$sourcePath' is not a valid directory")
        return BLINK_SOURCE_PATH
    }

    // create RAW and JPEG folders inside destination directory
    val createdRaw = File(destinationDir, "RAW").mkdirs()
    appLog("Created RAW dir: $createdRaw")
    val createdJpeg = File(destinationDir, "JPEG").mkdirs()
    appLog("Created JPEG dir: $createdJpeg")

    // extract file path list from source directory
    val files_list = getFilesList(sourcePath)
    if (files_list.isEmpty()) {
        appLog("No files found in source directory '$sourcePath'")
        return NO_FILES_FOUND
    }
    appLog("Found ${files_list.size} file(s).")
    // copy files to destination
    if (!deleteFiles){
        appLog("Files are copied to '$destinationDir'")
        copyFiles(files_list, destinationDir)
    }
    // move files from source
    if (deleteFiles){
        appLog("Files are moved to '$destinationDir'")
        moveFiles(files_list, destinationDir)
    }

    return NO_ERROR
}

@Composable
@Preview
fun App(
    showTerminalContent: Boolean = false,
    onToggleTerminalOutput: () -> Unit = {},
) {
    var albumName by remember { mutableStateOf("") }
    var albumPathSource by remember { mutableStateOf("") }
    var sourceBlinkTrigger by remember { mutableStateOf(0) }
    var highlightSourcePath by remember { mutableStateOf(false) }

    var albumPathDestination by remember { mutableStateOf(defaultPathDestination) }
    var destinationBlinkTrigger by remember { mutableStateOf(0) }
    var highlightDestinationPath by remember { mutableStateOf(false) }

    LaunchedEffect(destinationBlinkTrigger) {
        if (destinationBlinkTrigger == 0) {
            return@LaunchedEffect
        }

        repeat(7) {
            highlightDestinationPath = true
            delay(200.milliseconds)
            highlightDestinationPath = false
            delay(200.milliseconds)
        }
    }
    LaunchedEffect(sourceBlinkTrigger) {
        if (sourceBlinkTrigger == 0) {
            return@LaunchedEffect
        }

        repeat(7) {
            highlightSourcePath = true
            delay(200.milliseconds)
            highlightSourcePath = false
            delay(200.milliseconds)
        }
    }

    val destinationRowColor by animateColorAsState(
        targetValue = if (highlightDestinationPath) blinkColor else MaterialTheme.colorScheme.primary,
        label = "destinationRowColor"
    )
    val sourceRowColor by animateColorAsState(
        targetValue = if (highlightSourcePath) blinkColor else MaterialTheme.colorScheme.secondary,
        label = "sourceRowColor"
    )

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
            Row( // Destination path input
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. The Path Field
                OutlinedTextField(
                    value = albumPathDestination,
                    onValueChange = { albumPathDestination = it },
                    label = { Text("Destination Album Path") },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    readOnly = false, // Prevents manual typing if preferred
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = destinationRowColor,
                        unfocusedBorderColor = destinationRowColor,
                        focusedLabelColor = destinationRowColor,
                        unfocusedLabelColor = destinationRowColor,
                        focusedTextColor = destinationRowColor,
                        unfocusedTextColor = destinationRowColor,
                        focusedSupportingTextColor = destinationRowColor,
                        unfocusedSupportingTextColor = destinationRowColor,
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
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = destinationRowColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Open Explorer")
                }
            }
            Row( // Source path input
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. The Path Field
                OutlinedTextField(
                    value = albumPathSource,
                    onValueChange = { albumPathSource = it },
                    label = { Text("Source Album Path") },
                    modifier = Modifier.fillMaxWidth(0.7f),
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = sourceRowColor,
                        unfocusedBorderColor = sourceRowColor,
                        focusedLabelColor = sourceRowColor,
                        unfocusedLabelColor = sourceRowColor,
                        focusedTextColor = sourceRowColor,
                        unfocusedTextColor = sourceRowColor,
                        disabledTextColor = sourceRowColor,
                        focusedSupportingTextColor = sourceRowColor,
                        unfocusedSupportingTextColor = sourceRowColor,
                    )
                )

                // 2. Added a space of 8 dp between the path text field and Open button
                Spacer(modifier = Modifier.width(8.dp))

                // 3. The Explorer Button
                Button(
                    onClick = {
                        pickFolder(albumPathSource)?.let { selectedPath ->
                            albumPathSource = selectedPath
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = sourceRowColor,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    )
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Open Explorer")
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
                        if (shouldBlink == BLINK_DEST_PATH) {
                            destinationBlinkTrigger++
                        }
                        if (shouldBlink == BLINK_SOURCE_PATH) {
                            sourceBlinkTrigger++
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
                        if (shouldBlink == BLINK_DEST_PATH) {
                            destinationBlinkTrigger++
                        }
                        if (shouldBlink == BLINK_SOURCE_PATH) {
                            sourceBlinkTrigger++
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onToggleTerminalOutput) {
                    Text(
                        if (showTerminalContent) {
                            "Hide terminal output.."
                        } else {
                            "Show terminal output.."
                        }
                    )
                }
            }
            AnimatedVisibility(showTerminalContent) {
                Column(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .padding(bottom = 12.dp)
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight()
                        .heightIn(min = 300.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TerminalPanel(
                        modifier = Modifier.fillMaxSize(),
                        title = "photoorg.exe",
                        lines = AppConsole.lines,
                    )
                }
            }
        }
}
