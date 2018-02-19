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

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

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
fun AesKeyProperties.toSecretKey() =
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
        }.generateKey()
