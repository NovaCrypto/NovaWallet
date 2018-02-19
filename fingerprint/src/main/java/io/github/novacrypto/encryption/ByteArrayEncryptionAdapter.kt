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

fun Encryption.toByteArrayEncryption(): ByteArrayEncryption = ByteArrayEncryptionAdapter(this)

private class ByteArrayEncryptionAdapter(private val adapted: Encryption) : ByteArrayEncryption {

    override fun encrypt(plainData: ByteArray, onEncoded: (ByteArray) -> Unit) {
        adapted.encrypt(DataBlock.Plain(plainData)) {
            onEncoded(it.serialize())
        }
    }

    override fun decrypt(encodedIvAndData: ByteArray, onDecoded: (ByteArray) -> Unit) {
        val dataBlock = DataBlock.deserialize(encodedIvAndData)
        adapted.decrypt(dataBlock) {
            onDecoded(it.plainData)
        }
    }
}