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

import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode

internal class ClosestToCentreDetector(
        private val onNewBarcode: (Barcode) -> Unit,
        private val onNoBarcodeVisible: () -> Unit,
        private val getCentre: () -> Size
) : Detector.Processor<Barcode> {

    private val barcodes = HashMap<String, BarcodeDetail>()
    private val old = mutableListOf<String>()

    inner class BarcodeDetail(val barcode: Barcode, val time: Long)

    override fun release() {
        barcodes.clear()
    }

    override fun receiveDetections(detections: Detector.Detections<Barcode>) {
        val time = System.currentTimeMillis()

        addNewlyDetectedBarcodes(detections, time)

        val closest: Barcode? = findTheClosestNonExpiredBarcodeToCentre(time)

        removeOldBarcodes()

        if (closest != null) {
            onNewBarcode(closest)
        } else {
            onNoBarcodeVisible()
        }
    }

    private fun addNewlyDetectedBarcodes(detections: Detector.Detections<Barcode>, time: Long) {
        if (detections.detectedItems.size() > 0) {
            for (barcode in detections.detectedItems.toIterable()) {
                barcodes[barcode.displayValue] = BarcodeDetail(barcode, time)
            }
        }
    }

    private fun findTheClosestNonExpiredBarcodeToCentre(time: Long): Barcode? {
        val centre = getCentre()
        var closest: Barcode? = null
        var minD2: Int = Int.MAX_VALUE

        for (barcodeEntry in barcodes.entries) {
            if (barcodeEntry.value.time < time - 1000) {
                old.add(barcodeEntry.key)
            } else {
                val d2 = centre distanceSquared barcodeEntry.value.barcode.boundingBox
                if (d2 < minD2) {
                    minD2 = d2
                    closest = barcodeEntry.value.barcode
                }
            }
        }
        return closest
    }

    private fun removeOldBarcodes() {
        for (oldBarcode in old) {
            barcodes.remove(oldBarcode)
        }
        old.clear()
    }
}