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

import io.github.novacrypto.account.AccountModel
import io.github.novacrypto.account.renameAccount
import io.github.novacrypto.account.toAccountModelStream
import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class AccountModelTests {

    @Test
    fun `new account gets new id`() {
        AccountModel().id `should not equal` AccountModel().id
    }

}

class AccountModelRenameIntentTests {

    @Test
    fun `user can rename account`() {
        Observable.just(
                renameAccount("My Account")
        ).toAccountModelStream()
                .assertWithLastElement {
                    it.name `should equal` "My Account"
                }
    }

    @Test
    fun `user can rename account twice`() {
        Observable.just(
                renameAccount("My Account"),
                renameAccount("My Account 2")
        ).toAccountModelStream()
                .assertWithLastElement {
                    it.name `should equal` "My Account 2"
                }
                .assertWithLastTwoElements { penultimate, ultimate ->
                    penultimate.id `should be` ultimate.id
                }
    }
}