/*
 *  NovaWallet, Cryptocurrency Wallet for Android
 *  Copyright (C) 2018 Alan Evans, NovaCrypto
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *  Original source: https://github.com/NovaCrypto/NovaWallet
 *  You can contact the authors via github issues.
 */

package io.github.novacrypto.qrscanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import timber.log.Timber

internal class QrCamera(
        private val cameraSurfaceView: CameraSourcePreview,
        private val options: Options,
        private val onAccept: (String) -> Unit,
        filter: (String) -> Boolean
) {

    private val context = cameraSurfaceView.context.applicationContext
    private var cameraSource: CameraSource

    init {
        val detector = BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()
        cameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(1920, 1080)
                .setAutoFocusEnabled(true)
                .build()
        detector.setProcessor(ClosestToCentreDetector(
                { barcode ->
                    cameraSurfaceView.setRenderBarcode(barcode)
                    barcodeText = barcode.displayValue
                },
                {
                    cameraSurfaceView.setRenderBarcode(null)
                },
                filter
        )
        )
    }

    private var barcodeText: String? = null
        set(value) {
            if (value == field) return
            field = value
            value?.let {
                Snackbar.make(cameraSurfaceView, it, Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, { _ ->
                            Timber.d("Accepted barcode")
                            onAccept(it)
                        })
                        .show()
            }
        }

    fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraSurfaceView.start(cameraSource, options)
        } else {
            Timber.w("No permissions for camera")
            ActivityCompat.requestPermissions(cameraSurfaceView.context as Activity,
                    arrayOf(Manifest.permission.CAMERA),
                    ScanQrActivity.CAMERA_PERMISSION_RESPONSE)
        }
    }

    fun stop() {
        cameraSurfaceView.stop()
    }

    fun release() {
        cameraSurfaceView.release()
    }
}

