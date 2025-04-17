package photocopier

import kotlinx.coroutines.*
import java.io.File

interface ExternalDriveListener {
    fun onExternalDriveDetected(photosDir: File)
    fun onExternalDriveDebug(info: String)
}

class ExternalDriveWatcher(private val listener: ExternalDriveListener) {
    private var watcherJob: Job? = null

    fun startWatching() {
        watcherJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val volumes = File("/Volumes").listFiles()?.filter { it.isDirectory && it.name != "Macintosh HD" }
                val debugInfo = StringBuilder()
                debugInfo.append("Volumes found:\n")
                volumes?.forEach { volume ->
                    debugInfo.append("- ${'$'}{volume.absolutePath}\n")
                    val photosDir = File(volume, "photos")
                    if (photosDir.exists() && photosDir.isDirectory) {
                        listener.onExternalDriveDetected(photosDir)
                        listener.onExternalDriveDebug("Auto-detected external drive: ${'$'}{photosDir.absolutePath}")
                        return@launch
                    }
                }
                listener.onExternalDriveDebug(debugInfo.toString())
                delay(2000)
            }
        }
    }

    fun stopWatching() {
        watcherJob?.cancel()
    }
}
