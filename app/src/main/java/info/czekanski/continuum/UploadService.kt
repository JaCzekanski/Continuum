package info.czekanski.continuum

import android.app.IntentService
import android.content.Intent
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class UploadService : IntentService("Continuum") {
    private val continuumService = ContinuumService(this)
    private lateinit var receiver: NsdServiceInfo

    override fun onHandleIntent(intent: Intent?) {
        val receiver = runBlocking {
            withTimeoutOrNull(5 * 1000) {
                continuumService.findReceiver()
            }
        }

        if (receiver == null) {
            Log.d("UploadService", "Continuum receiver not found")
            Toast.makeText(this, "Continuum receiver not found", Toast.LENGTH_SHORT).show()
            return
        }
        this.receiver = receiver

        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                sendFile(uri ?: return)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                uris?.forEach(::sendFile)
            }
        }
    }

    private fun sendFile(uri: Uri) {
        Log.d("SendActivity", "uri: $uri")
        if (uri.scheme != "content") {
            Log.d("SendActivity", "Scheme != content, not supported")
            Toast.makeText(this, "Scheme != content, not supported", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = contentResolver.query(uri, null, null, null, null)?.use {
            it.moveToFirst()
            it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
        val data = contentResolver.openInputStream(uri)?.use {
            it.readBytes()
        }

        if (filename == null || data == null) {
            Log.d("UploadService", "filename == null || data == null")
            return
        }

        runBlocking {
            try {
                continuumService.send(receiver, filename, data)
            } catch (e: Throwable) {
                Log.d("UploadService", "Upload failed", e)
                showError("Upload failed")
            }
        }
    }

    private suspend fun showError(text: String) = withContext(Dispatchers.Main) {
        Toast.makeText(this@UploadService, text, Toast.LENGTH_SHORT).show()
    }
}
