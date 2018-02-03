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

class Base16Tests {

    @Test
    fun `empty encode`() {
        Base16().encode(byteArrayOf()) `should equal` ""
    }

    @Test
    fun `empty decode`() {
        Base16().decode("") `should equal` byteArrayOf()
    }

    @Test
    fun `encode 1 byte`() {
        Base16().encode(byteArrayOf(1)) `should equal` "01"
    }

    @Test
    fun `decode 1 byte`() {
        Base16().decode("01") `should equal` byteArrayOf(1)
    }

    @Test
    fun `encode all values byte`() {
        val base16 = Base16()
        for (i in 0..255) {
            val singleHex = String.format("%02x", i)
            base16.encode(byteArrayOf(i.toByte())) `should equal` singleHex
        }
    }

    @Test
    fun `decode all values byte`() {
        val base16 = Base16()
        for (i in 0..255) {
            base16.decode(base16.encode(byteArrayOf(i.toByte()))) `should equal` byteArrayOf(i.toByte())
        }
    }

    @Test
    fun `decode all values byte in second position`() {
        val base16 = Base16()
        for (i in 0..255) {
            base16.decode(base16.encode(byteArrayOf(17, i.toByte()))) `should equal` byteArrayOf(17, i.toByte())
        }
    }
}