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

package io.github.novacrypto.fingerprint

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@RequiresApi(Build.VERSION_CODES.M)
class FingerPrintEncryption(
        context: Context,
        private val keyName: String,
        private val transform: String,
        private val onAuthorizeFailure: (() -> Unit)? = null
) {

    private var manager = FingerprintManagerCompat.from(context)

    private val DATA_VERSION = 1.toByte()

    fun encode(initialData: ByteArray, onEncoded: (ByteArray) -> Unit) {
        val cryptoObject = getSecretKey(keyName)
                .toEncryptCypher()
                .toCryptoObject()

        authorize(cryptoObject, initialData) { data: ByteArray, iv: ByteArray ->
            onEncoded(byteArrayOf(DATA_VERSION, iv.size.toByte()) + iv + data)
        }
    }

    fun decode(encodedDataAndIv: ByteArray, onDecoded: (ByteArray) -> Unit) {
        val dataVersion = encodedDataAndIv[0]
        if (dataVersion != DATA_VERSION) {
            throw Exception("Can't decode data of version $dataVersion, expecting $DATA_VERSION")
        }
        val ivSize = encodedDataAndIv[1]
        val iv = encodedDataAndIv.sliceArray(2 until ivSize + 2)
        val encodedData = encodedDataAndIv.sliceArray(ivSize + 2 until encodedDataAndIv.size)
        val secretKey = getSecretKey(keyName)
        val cryptoObject = secretKey
                .toDecryptCypher(iv)
                .toCryptoObject()

        authorize(cryptoObject, encodedData) { data: ByteArray, _: ByteArray ->
            onDecoded(data)
        }
    }

    private fun authorize(
            cryptoObject: FingerprintManagerCompat.CryptoObject,
            data: ByteArray,
            onAuthorized: (data: ByteArray, iv: ByteArray) -> Unit
    ) {
        manager.authenticate(cryptoObject, 0, null,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onAuthorized(cryptoObject.cipher.doFinal(data), cryptoObject.cipher.iv)
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onAuthorizeFailure?.invoke()
                    }
                }, null)
    }

    private fun SecretKey.toEncryptCypher() =
            Cipher.getInstance(transform)
                    .also {
                        it.init(Cipher.ENCRYPT_MODE, this)
                    }

    private fun Cipher.toCryptoObject() = FingerprintManagerCompat.CryptoObject(this)

    private fun SecretKey.toDecryptCypher(iv: ByteArray) =
            Cipher.getInstance(transform)
                    .also {
                        it.init(Cipher.DECRYPT_MODE, this, IvParameterSpec(iv))
                    }

    private fun getSecretKey(keyName: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val key = keyStore.getKey(keyName, null) as SecretKey?
        if (key != null) {
            Log.d("ALAN", "Using existing key")
        }
        return key ?: generateKey(keyName)
    }

    private fun generateKey(keyName: String) =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(keyName,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        // Require the user to authenticate with a fingerprint to authorize every use
                        // of the key
                        .setUserAuthenticationRequired(true)

                        //If we use this, then you can use the system auth, otherwise you must use your own
                        //.setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build())
                Log.d("ALAN", "New key")
            }.generateKey()
}