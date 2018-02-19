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

package io.github.novacrypto.encryption.fingerprint

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import io.github.novacrypto.encryption.DataBlock
import io.github.novacrypto.encryption.Encryption
import io.github.novacrypto.encryption.AesKey

@RequiresApi(Build.VERSION_CODES.M)
class FingerPrintEncryption(
        context: Context,
        private val key: AesKey,
        private val onAuthorizeFailure: (() -> Unit)? = null
) : Encryption {

    private var manager = FingerprintManagerCompat.from(context)

    override fun encrypt(plainData: DataBlock.Plain, onEncoded: (DataBlock.Encoded) -> Unit) {
        val cryptoObject = key
                .toEncryptCryptoObject()

        authorize(cryptoObject, plainData) { data: DataBlock ->
            when (data) {
                is DataBlock.Encoded -> onEncoded(data)
                else -> throw Exception("Expected Encoded back")
            }
        }
    }

    override fun decrypt(encodedData: DataBlock.Encoded, onDecoded: (DataBlock.Plain) -> Unit) {
        val cryptoObject = key.toDecryptCryptoObject(encodedData.iv)

        authorize(cryptoObject, encodedData) { data: DataBlock ->
            when (data) {
                is DataBlock.Plain -> onDecoded(data)
                else -> throw Exception("Expected decoded back")
            }
        }
    }

    private fun authorize(
            cryptoObject: FingerprintManagerCompat.CryptoObject,
            data: DataBlock,
            onAuthorized: (DataBlock) -> Unit
    ) {
        manager.authenticate(cryptoObject, 0, null,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        when (data) {
                            is DataBlock.Plain ->
                                onAuthorized(DataBlock.Encoded(
                                        cryptoObject.cipher.iv,
                                        cryptoObject.cipher.doFinal(data.plainData)))
                            is DataBlock.Encoded ->
                                onAuthorized(DataBlock.Plain(cryptoObject.cipher.doFinal(data.encodedData)))
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onAuthorizeFailure?.invoke()
                    }
                }, null)
    }
}