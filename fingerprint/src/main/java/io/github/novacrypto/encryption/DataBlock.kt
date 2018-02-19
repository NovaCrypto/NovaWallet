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

package io.github.novacrypto.encryption

sealed class DataBlock {
    class Encoded(
            val iv: ByteArray,
            val encodedData: ByteArray
    ) : DataBlock() {

        fun serialize(): ByteArray {
            return byteArrayOf(DATA_VERSION, iv.size.toByte()) + iv + encodedData
        }
    }

    class Plain(val plainData: ByteArray) : DataBlock()

    companion object {
        private const val DATA_VERSION = 1.toByte()

        fun deserialize(encodedIvAndData: ByteArray): Encoded {
            val dataVersion = encodedIvAndData[0]
            when (dataVersion) {
                1.toByte() -> {
                    val ivSize = encodedIvAndData[1]
                    val iv = encodedIvAndData.sliceArray(2 until ivSize + 2)
                    val encodedData = encodedIvAndData.sliceArray(ivSize + 2 until encodedIvAndData.size)
                    return Encoded(iv, encodedData)
                }
                else -> throw Exception("Can't decode data of version $dataVersion, expecting ${DATA_VERSION}")
            }
        }
    }
}