package info.czekanski.continuum

import android.app.Activity
import android.os.Bundle

class UploadActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(intent.setClass(this, UploadService::class.java))
        finish()
    }
}
