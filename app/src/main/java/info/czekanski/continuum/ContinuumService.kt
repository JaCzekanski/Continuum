package info.czekanski.continuum

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ContinuumService(context: Context) {
    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }

    suspend fun findReceiver(): NsdServiceInfo = suspendCancellableCoroutine { cont ->
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("Continuum", "onDiscoveryStarted(serviceType: $serviceType)")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("Continuum", "onDiscoveryStopped(serviceType: $serviceType)")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d("Continuum", "onServiceFound(serviceInfo: $serviceInfo)")
                cont.resume(serviceInfo)
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d("Continuum", "onServiceLost(serviceInfo: $serviceInfo)")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.d("Continuum", "onStartDiscoveryFailed(serviceType: $serviceType, errorCode: $errorCode)")
                cont.cancel()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.d("Continuum", "onStopDiscoveryFailed(serviceType: $serviceType, errorCode: $errorCode)")
                cont.cancel()
            }
        }

        nsdManager.discoverServices(CONTINUUM_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        cont.invokeOnCancellation {
            Log.d("Continuum", "findReceiver cancelled")
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
    }

    private suspend fun resolve(serviceInfo: NsdServiceInfo): NsdServiceInfo? = suspendCoroutine { cont ->
        val resolver = object : NsdManager.ResolveListener {
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                Log.d("Continuum", "onServiceResolved(serviceInfo: $serviceInfo)")
                cont.resume(serviceInfo)
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.d("Continuum", "onResolveFailed(serviceInfo: $serviceInfo, errorCode: $errorCode)")
                cont.resume(null)
            }
        }

        nsdManager.resolveService(serviceInfo, resolver)
    }

    suspend fun send(serviceInfo: NsdServiceInfo, filename: String, data: ByteArray) {
        val resolved = resolve(serviceInfo) ?: return

        Log.d("Continuum", "Sending $filename (size: ${data.size})")
        withContext(Dispatchers.IO) {
            val url = URL("http", resolved.host.hostAddress, resolved.port, "upload")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.doOutput = true
            con.connectTimeout = 1000
            con.setRequestProperty("filename", filename)

            DataOutputStream(con.outputStream).use {
                it.write(data)
            }
            val response = con.inputStream.bufferedReader().use { it.readText() }
            Log.d("Continuum", "Done")
        }
    }

    companion object {
        const val CONTINUUM_SERVICE_TYPE = "_continuum._tcp"
        const val CONTINUUM_PORT = 49646
    }
}
