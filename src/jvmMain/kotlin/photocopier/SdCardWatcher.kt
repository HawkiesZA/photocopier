package photocopier

import kotlinx.coroutines.*
import java.io.File

interface SdCardListener {
    fun onSdCardDetected(dcimFolder: File)
    fun onWatcherDebug(info: String)
}

class SdCardWatcher(private val listener: SdCardListener) {
    private var watcherJob: Job? = null

    fun startWatching() {
        watcherJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val volumes = File("/Volumes").listFiles()?.filter { it.isDirectory && it.name != "Macintosh HD" }
                val debugInfo = StringBuilder()
                debugInfo.append("Volumes found:\n")
                volumes?.forEach { volume ->
                    debugInfo.append("- ${'$'}{volume.absolutePath}\n")
                    val dcimFolder = File(volume, "DCIM")
                    if (dcimFolder.exists() && dcimFolder.isDirectory) {
                        val subfolders = dcimFolder.listFiles()?.filter { it.isDirectory }
                        val subfolderNames = subfolders?.joinToString(separator = ", ") { it.name } ?: "none"
                        debugInfo.append("  DCIM subfolders: $subfolderNames\n")
                        subfolders?.forEach { sub ->
                            if (sub.listFiles()?.isNotEmpty() == true) {
                                listener.onSdCardDetected(sub)
                                listener.onWatcherDebug("Auto-detected: ${'$'}{sub.absolutePath}")
                                return@launch
                            }
                        }
                    }
                }
                listener.onWatcherDebug(debugInfo.toString())
                delay(2000)
            }
        }
    }

    fun stopWatching() {
        watcherJob?.cancel()
    }
}
