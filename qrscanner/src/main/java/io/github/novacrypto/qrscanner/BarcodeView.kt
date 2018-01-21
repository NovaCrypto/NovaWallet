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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.barcode.Barcode

internal class BarcodeView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var renderBarcode: Barcode? = null
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint()
            .apply {
                color = Color.GREEN
                style = Paint.Style.STROKE
                strokeWidth = 3f
            }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        renderBarcode?.let {
            canvas.save()
            previewSize?.swapIfOrientationDifferent(width, height)?.let {
                canvas.scale(width.toFloat() / it.width, height.toFloat() / it.height)
            }
            canvas.drawRect(it.boundingBox, paint)
            canvas.restore()
        }
    }

    var previewSize: Size? = null
        set(value) {
            if (value == field) return
            field = value
            invalidate()
        }
}