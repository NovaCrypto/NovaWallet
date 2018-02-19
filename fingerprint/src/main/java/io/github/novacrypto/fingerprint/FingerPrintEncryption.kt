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

interface ByteArrayEncryption {
    fun encrypt(plainData: ByteArray, onEncoded: (encodedIvAndData: ByteArray) -> Unit)
    fun decrypt(encodedIvAndData: ByteArray, onDecoded: (data: ByteArray) -> Unit)
}

interface Encryption {
    fun encrypt(plainData: DataBlock.Plain, onEncoded: (encodedData: DataBlock.Encoded) -> Unit)
    fun decrypt(encodedData: DataBlock.Encoded, onDecoded: (data: DataBlock.Plain) -> Unit)
}

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
                else -> throw Exception("Can't decode data of version $dataVersion, expecting $DATA_VERSION")
            }
        }
    }
}

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

@RequiresApi(Build.VERSION_CODES.M)
class AesKeyProperties(
        val keyName: String,
        val blockMode: String = KeyProperties.BLOCK_MODE_CBC,
        val padding: String = KeyProperties.ENCRYPTION_PADDING_PKCS7
) {
    val algorithm = KeyProperties.KEY_ALGORITHM_AES
    val transform: String = "$algorithm/$blockMode/$padding"
}

@RequiresApi(Build.VERSION_CODES.M)
class AesKey(
        private val properties: AesKeyProperties
) {
    fun toEncryptCryptoObject() =
            properties.toSecretKey()
                    .toEncryptCypher()
                    .toCryptoObject()

    fun toDecryptCryptoObject(iv: ByteArray) =
            properties.toSecretKey()
                    .toDecryptCypher(iv)
                    .toCryptoObject()

    private fun SecretKey.toEncryptCypher() =
            newUninitializedCipher()
                    .also {
                        it.init(Cipher.ENCRYPT_MODE, this)
                    }

    private fun SecretKey.toDecryptCypher(iv: ByteArray) =
            newUninitializedCipher()
                    .also {
                        it.init(Cipher.DECRYPT_MODE, this, IvParameterSpec(iv))
                    }

    private fun newUninitializedCipher() = Cipher.getInstance(properties.transform)
}

private fun Cipher.toCryptoObject() = FingerprintManagerCompat.CryptoObject(this)

@RequiresApi(Build.VERSION_CODES.M)
private fun AesKeyProperties.toSecretKey() =
        loadSecretKey() ?: generateAndStoreSecretKey()

private fun AesKeyProperties.loadSecretKey(): SecretKey? =
        KeyStore.getInstance("AndroidKeyStore")
                .apply {
                    load(null)
                }.getKey(keyName, null) as SecretKey?

@RequiresApi(Build.VERSION_CODES.M)
private fun AesKeyProperties.generateAndStoreSecretKey(): SecretKey =
        KeyGenerator.getInstance(algorithm, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(blockMode)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)

                    //If we use this, then you can use the system auth, otherwise you must use your own
                    //.setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(padding)
                    .build())
            Log.d("ALAN", "New key")
        }.generateKey()
