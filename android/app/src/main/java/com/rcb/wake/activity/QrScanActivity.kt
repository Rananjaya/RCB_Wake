package com.rcb.wake.activity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.rcb.wake.R
import com.rcb.wake.alarm.AlarmSessionStore
import com.rcb.wake.service.RingingService

/**
 * QR wake-up challenge. Scanning any QR code dismisses the ringing session. Frames are decoded on
 * the device by ZXing and never stored or sent; the decoded text is not kept either.
 * Shown over the lock screen so the user does not have to unlock first.
 */
class QrScanActivity : Activity() {
    private lateinit var scanner: DecoratedBarcodeView
    private var done = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (AlarmSessionStore.load(this) == null) return finish()

        scanner = DecoratedBarcodeView(this)
        scanner.barcodeView.decoderFactory = DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        scanner.setStatusText(getString(R.string.qr_scan_prompt))
        setContentView(scanner)
        scanner.decodeContinuous { result ->
            if (done || result.text.isNullOrEmpty()) return@decodeContinuous
            done = true
            scanner.pause()
            startService(RingingService.dismissIntent(this, RingingService.METHOD_CHALLENGE))
            finish()
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CAMERA) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            scanner.resume()
        } else {
            Toast.makeText(this, R.string.qr_camera_denied, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::scanner.isInitialized && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            scanner.resume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::scanner.isInitialized) scanner.pause()
    }

    private companion object {
        const val REQUEST_CAMERA = 41
    }
}
