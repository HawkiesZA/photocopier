package photocopier

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JFileChooser
import javax.swing.UIManager
import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifSubIFDDirectory

fun selectDirectory(title: String): File? {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    val chooser = JFileChooser()
    chooser.dialogTitle = title
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val result = chooser.showOpenDialog(null)
    return if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
}

fun extractPhotoDate(file: File): String {
    return try {
        val metadata = ImageMetadataReader.readMetadata(file)
        val exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java)
        val date = exifDir?.getDateOriginal() ?: exifDir?.getDateDigitized()
        val sdfYear = SimpleDateFormat("yyyy", Locale.US)
        val sdfMonthDay = SimpleDateFormat("MM-dd", Locale.US)
        if (date != null) {
            "${sdfYear.format(date)}/${sdfMonthDay.format(date)}"
        } else {
            // Fallback: use file last modified date
            val fallback = Date(file.lastModified())
            "${sdfYear.format(fallback)}/${sdfMonthDay.format(fallback)}"
        }
    } catch (e: Exception) {
        // Fallback: use file last modified date
        val fallback = Date(file.lastModified())
        val sdfYear = SimpleDateFormat("yyyy", Locale.US)
        val sdfMonthDay = SimpleDateFormat("MM-dd", Locale.US)
        "${sdfYear.format(fallback)}/${sdfMonthDay.format(fallback)}"
    }
}

@Composable
fun GradientHeadline() {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color.Blue,
            Color(0xFF0086FF), // blue
            Color(0xFF6D5BFF), // blue-purple
            Color(0xFF9B5CFF), // purple
            Color(0xFFFF4BCD)  // pink
        ),
        start = Offset(0f, 0f),
        end = Offset(1000f, 0f)
    )
    Text(
        text = "Photocopier",
        modifier = Modifier.fillMaxWidth(),
        style = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 48.sp,
            brush = gradient,
            textAlign = TextAlign.Center
        ),
        maxLines = 1
    )
}

@Composable
fun App() {
    var sourceDir by remember { mutableStateOf<File?>(null) }
    var targetDir by remember { mutableStateOf<File?>(null) }
    var isCopying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val sdCardListener = object : SdCardListener {
            override fun onSdCardDetected(dcimFolder: File) {
                coroutineScope.launch {
                    sourceDir = dcimFolder
                    status = "SD Card detected: ${dcimFolder.absolutePath}"
                }
            }
            override fun onWatcherDebug(info: String) {
                coroutineScope.launch {
                    println(info)
                }
            }
        }
        val externalDriveListener = object : ExternalDriveListener {
            override fun onExternalDriveDetected(photosDir: File) {
                coroutineScope.launch {
                    targetDir = photosDir
                    status = "External drive detected: ${photosDir.absolutePath}"
                }
            }
            override fun onExternalDriveDebug(info: String) {
                coroutineScope.launch {
                    println(info)
                }
            }
        }
        val sdCardWatcher = SdCardWatcher(sdCardListener)
        val externalDriveWatcher = ExternalDriveWatcher(externalDriveListener)
        sdCardWatcher.startWatching()
        externalDriveWatcher.startWatching()
        onDispose {
            sdCardWatcher.stopWatching()
            externalDriveWatcher.stopWatching()
        }
    }

    MaterialTheme {
        Column(modifier = Modifier.padding(24.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            GradientHeadline()
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        selectDirectory("Select SD Card Source")?.let { sourceDir = it }
                    },
                    enabled = !isCopying,
                    modifier = Modifier.width(220.dp)
                ) { Text("Select SD Card") }
                Spacer(Modifier.width(8.dp))
                Text(sourceDir?.absolutePath ?: "No source selected")
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        selectDirectory("Select Target Directory")?.let { targetDir = it }
                    },
                    enabled = !isCopying,
                    modifier = Modifier.width(220.dp)
                ) { Text("Select Target Dir") }
                Spacer(Modifier.width(8.dp))
                Text(targetDir?.absolutePath ?: "No target selected")
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (sourceDir != null && targetDir != null) {
                    isCopying = true
                    progress = 0
                    status = ""
                    CoroutineScope(Dispatchers.IO).launch {
                        val files = sourceDir!!.walk().filter {
                            it.isFile && (it.extension.lowercase() in listOf("jpg","jpeg","png","cr2","nef","arw","rw2"))
                        }.toList()
                        total = files.size
                        for ((i, file) in files.withIndex()) {
                            val date = extractPhotoDate(file)
                            val destDir = File(targetDir, date)
                            destDir.mkdirs()
                            val destFile = File(destDir, file.name)
                                if (!destFile.exists()) {
                                    try {
                                        file.copyTo(destFile, overwrite = false)
                                    } catch (e: Exception) {
                                        // Optionally log or show error
                                        println("Error copying ${file.name}: ${e.message}")
                                    }
                                }
                            progress = i + 1
                            status = "Copying ${file.name} ($progress/$total)"
                        }
                        isCopying = false
                        status = "Done!"
                    }
                }
            },
                enabled = !isCopying && sourceDir != null && targetDir != null,
                shape = RoundedCornerShape(50),
                elevation = ButtonDefaults.elevation(defaultElevation = 10.dp, pressedElevation = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF0A84FF), // Apple system blue
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .width(220.dp)
                    .height(64.dp)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    "Copy Photos",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(16.dp))
            if (isCopying || progress > 0) {
                LinearProgressIndicator(progress = if (total > 0) progress / total.toFloat() else 0f, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(status)
            }
        }
    }
}
