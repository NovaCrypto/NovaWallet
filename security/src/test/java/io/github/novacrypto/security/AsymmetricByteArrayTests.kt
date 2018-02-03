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

import org.amshove.kluent.`should equal`
import org.junit.Test

class AsymmetricByteArrayTests {

    @Test
    fun `can encode and decode a byte array`() {
        val security = AsymmetricSecurity()

        val key = security.publicKeyAsString
        val encoder: Encoder = AsymmetricSecurity.encoder(key)

        val encoded = encoder.encode(byteArrayOf(0, 2, 3, 4, -128, 127))

        val decoder: Decoder = security.decoder()
        decoder.decodeByteArray(encoded) `should equal` byteArrayOf(0, 2, 3, 4, -128, 127)
    }

    @Test
    fun `can encode and decode a two byte arrays`() {
        val security = AsymmetricSecurity()

        val key = security.publicKeyAsString
        val encoder: Encoder = AsymmetricSecurity.encoder(key)

        val encoded1 = encoder.encode(byteArrayOf(0, 2, 3, 4, -128, 127))
        val encoded2 = encoder.encode(byteArrayOf(-2, 43, 74, 127))

        val decoder: Decoder = security.decoder()
        decoder.decodeByteArray(encoded1) `should equal` byteArrayOf(0, 2, 3, 4, -128, 127)
        decoder.decodeByteArray(encoded2) `should equal` byteArrayOf(-2, 43, 74, 127)
    }
}