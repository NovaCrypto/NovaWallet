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

package io.github.novacrypto.security

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

interface PublicKeyEncoded {
    val publicKeyAsString: String
}

interface Encoder {
    fun encode(message: String): String
    fun encode(byteArray: ByteArray): String
}

interface Decoder {
    fun decodeString(encoded: String): String
    fun decodeByteArray(encoded: String): ByteArray
}

class AsymmetricSecurity : PublicKeyEncoded {
    private val base16 = Base16()

    companion object {
        fun encoder(key: String): Encoder {
            return AsymmetricEncoder(key)
        }
    }

    fun decoder(): Decoder {
        return object : Decoder {
            private val base16 = Base16()

            private val cipher = Cipher.getInstance("RSA").apply {
                init(Cipher.DECRYPT_MODE, privateKey)
            }

            override fun decodeByteArray(encoded: String) =
                    cipher.doFinal(base16.decode(encoded))

            override fun decodeString(encoded: String) =
                    String(decodeByteArray(encoded))
        }
    }

    private var privateKey: PrivateKey

    override val publicKeyAsString: String

    init {
        val kpg = KeyPairGenerator.getInstance("RSA").apply {
            initialize(1024)
        }
        val kp = kpg.genKeyPair()
        privateKey = kp.private
        publicKeyAsString = base16.encode(kp.public.encoded)
    }
}

private class AsymmetricEncoder(key: String) : Encoder {
    private val base16 = Base16()

    private var cipher: Cipher = Cipher.getInstance("RSA").apply {
        init(Cipher.ENCRYPT_MODE, readPublicKey(key))
    }

    override fun encode(byteArray: ByteArray): String {
        return base16.encode(cipher.doFinal(byteArray))
    }

    override fun encode(message: String): String {
        return encode(message.toByteArray())
    }

    private fun readPublicKey(key: String): PublicKey {
        return KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(base16.decode(key)))
    }
}