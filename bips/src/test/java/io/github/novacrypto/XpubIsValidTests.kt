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

package io.github.novacrypto

import io.github.novacrypto.bips.isValidXPub
import org.amshove.kluent.`should be`
import org.junit.Test

class ValidXpubTests {

    @Test
    fun `xpub is valid`() {
        "xpub68UrM5VsVKymX9zLuvi1ZkAfgTLqd8iLuyzEYU8VprQghHVAkH9es3KVfFyLJkCnnJj1prShvK5GF9wQRvDVUXE7ZuDGgVPZ5C6kALWVfjH"
                .isValidXPub() `should be` true
    }

    @Test
    fun `xpub with modified checksum is not valid`() {
        "xpub68UrM5VsVKymX9zLuvi1ZkAfgTLqd8iLuyzEYU8VprQghHVAkH9es3KVfFyLJkCnnJj1prShvK5GF9wQRvDVUXE7ZuDGgVPZ5C6kALWVfjh"
                .isValidXPub() `should be` false
    }

    @Test
    fun `xprv is not valid`() {
        "xprv9uVVwZxyexRUJfusouB1CcDw8RWMDfzVYm4dk5itGWshpVA2CjqQKF11oyoML33sZ4YpUwBTu8YNeawsPF3ctX6DuPmjSDt1rqAcUYVptHR"
                .isValidXPub() `should be` false
    }
}