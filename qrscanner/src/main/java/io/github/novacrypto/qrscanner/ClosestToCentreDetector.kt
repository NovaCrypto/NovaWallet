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

import android.graphics.Point
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode

internal class ClosestToCentreDetector(
        private val onNewBarcode: (Barcode) -> Unit,
        private val onNoBarcodeVisible: () -> Unit,
        private val filter: (String) -> Boolean
) : Detector.Processor<Barcode> {

    private val barcodes = HashMap<String, BarcodeDetail>()

    inner class BarcodeDetail(val barcode: Barcode, val expireTime: Long)

    override fun release() {
        barcodes.clear()
    }

    override fun receiveDetections(detections: Detector.Detections<Barcode>) {
        val time = System.currentTimeMillis()

        addNewlyDetectedBarcodes(detections, time)

        val closest: Barcode? = findTheClosestNonExpiredBarcodeToCentre(time,
                Point(detections.frameMetadata.width / 2,
                        detections.frameMetadata.height / 2))

        if (closest != null) {
            onNewBarcode(closest)
        } else {
            onNoBarcodeVisible()
        }
    }

    private fun addNewlyDetectedBarcodes(detections: Detector.Detections<Barcode>, time: Long) {
        if (detections.detectedItems.size() > 0) {
            for (barcode in detections.detectedItems.toIterable()) {
                barcodes[barcode.displayValue] = BarcodeDetail(barcode, time + 1000)
            }
        }
    }

    private fun findTheClosestNonExpiredBarcodeToCentre(time: Long, centre: Point): Barcode? {
        var closest: Barcode? = null
        var minD2: Int = Int.MAX_VALUE

        for (barcodeEntry in barcodes.entries) {
            if (barcodeEntry.value.expireTime > time) {
                val barcode = barcodeEntry.value.barcode
                if (!filter(barcode.displayValue)) {
                    continue
                }
                val boundingBox = barcode.boundingBox
                if (centre isInside boundingBox) return barcode
                val d2 = centre distanceSquared boundingBox
                if (d2 < minD2) {
                    minD2 = d2
                    closest = barcode
                }
            }
        }
        return closest
    }
}