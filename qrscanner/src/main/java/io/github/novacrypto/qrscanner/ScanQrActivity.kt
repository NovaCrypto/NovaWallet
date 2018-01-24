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

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_scan_qr.*
import kotlinx.android.synthetic.main.content_scan_qr.*
import timber.log.Timber

open class ScanQrActivity : AppCompatActivity() {

    companion object {
        const val OPTION_SHOW_BARCODE_BOX = "SHOW_BARCODE_BOX"
        const val BARCODE_DATA = "BARCODE_DATA"
        internal const val CAMERA_PERMISSION_RESPONSE = 23
    }

    private lateinit var qrCamera: QrCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan_qr)
        setSupportActionBar(toolbar)

        qrCamera = QrCamera(cameraPreview,
                intent.toOptions(),
                { barcode ->
                    setResult(RESULT_OK, Intent()
                            .apply {
                                putExtra(BARCODE_DATA, barcode)
                            })
                    finish()
                },
                getBarcodeFilter())
    }

    protected open fun getBarcodeFilter(): (CharSequence) -> Boolean {
        return { _ -> true }
    }

    override fun onResume() {
        super.onResume()
        qrCamera.start()
    }

    override fun onPause() {
        qrCamera.stop()
        super.onPause()
    }

    override fun onDestroy() {
        qrCamera.release()
        super.onDestroy()
    }

    private fun Intent?.toOptions() =
            Options(this?.extras?.getBoolean(OPTION_SHOW_BARCODE_BOX) ?: false)

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_RESPONSE -> {
                if (grantResults.contains(PackageManager.PERMISSION_GRANTED)) {
                    Timber.d("Camera permission granted")
                    qrCamera.start()
                } else {
                    Timber.e("Camera permission denied")
                    finish()
                }
            }
        }
    }
}