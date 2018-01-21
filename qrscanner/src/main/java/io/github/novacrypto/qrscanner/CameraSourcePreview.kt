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
import android.content.Context
import android.support.annotation.RequiresPermission
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.Barcode
import timber.log.Timber
import java.io.IOException

class CameraSourcePreview @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val surfaceView: SurfaceView

    private var startRequested: Boolean = false
    private var surfaceAvailable: Boolean = false
    private var cameraSource: CameraSource? = null

    private var barcodeView: BarcodeView? = null

    init {
        startRequested = false
        surfaceAvailable = false

        surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(SurfaceCallback())
        addView(surfaceView, 0)
    }

    private var showBarcodeBounds: Boolean = false
        set(value) {
            if (value == field) return
            field = value
            if (value) {
                barcodeView = BarcodeView(context)
                addView(barcodeView, 1)
            } else {
                barcodeView?.let { removeView(it) }
                barcodeView = null
            }
        }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Throws(SecurityException::class)
    internal fun start(cameraSource: CameraSource?, options: Options) {
        showBarcodeBounds = options.showBarcodeBounds
        if (this.cameraSource != cameraSource) {
            stop()

            this.cameraSource = cameraSource
                    ?.also {
                        startRequested = true
                        startIfReady()
                    }
        }
    }

    internal fun release() {
        cameraSource?.release()
        cameraSource = null
    }

    internal fun stop() {
        cameraSource?.stop()
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    @Throws(SecurityException::class)
    private fun startIfReady() {
        cameraSource?.let {
            if (startRequested && surfaceAvailable) {
                it.start(surfaceView.holder)
                startRequested = false
                barcodeView?.previewSize = it.previewSize
            }
        }
    }

    private inner class SurfaceCallback : SurfaceHolder.Callback {

        override fun surfaceCreated(surface: SurfaceHolder) {
            surfaceAvailable = true
            try {
                startIfReady()
            } catch (se: SecurityException) {
                Timber.e(se, "Do not have permission to start the camera")
            } catch (e: IOException) {
                Timber.e(e, "Could not start camera source.")
            }

        }

        override fun surfaceDestroyed(surface: SurfaceHolder) {
            surfaceAvailable = false
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        }
    }

    internal fun setRenderBarcode(valueAt: Barcode?) {
        barcodeView?.let {
            handler.post {
                it.renderBarcode = valueAt
            }
        }
    }

    private val defaultSize = Size(320, 240)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val viewWidth = right - left
        val viewHeight = bottom - top

        val previewSize = (cameraSource?.previewSize ?: defaultSize).swapIfOrientationDifferent(viewWidth, viewHeight)

        val childWidth: Int
        val childHeight: Int
        var childXOffset = 0
        var childYOffset = 0

        if (viewWidth * previewSize.height > viewHeight * previewSize.width) {
            childWidth = viewWidth
            val widthRatio = viewWidth.toFloat() / previewSize.width.toFloat()
            childHeight = (previewSize.height.toFloat() * widthRatio).toInt()
            childYOffset = (childHeight - viewHeight) / 2
        } else {
            val heightRatio = viewHeight.toFloat() / previewSize.height.toFloat()
            childWidth = (previewSize.width.toFloat() * heightRatio).toInt()
            childHeight = viewHeight
            childXOffset = (childWidth - viewWidth) / 2
        }

        for (i in 0 until childCount) {
            // One dimension will be cropped.  We shift child over or up by this offset and adjust
            // the size to maintain the proper aspect ratio.
            getChildAt(i).layout(
                    -childXOffset, -childYOffset,
                    childWidth - childXOffset, childHeight - childYOffset)
        }
    }
}