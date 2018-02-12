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

import io.github.novacrypto.account.*
import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.junit.Test

class UserModelIntentTests {

    @Test
    fun `initial user state`() {
        Observable.empty<UserModelViewStateIntent>()
                .toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` emptyList()
                }
    }

    @Test
    fun `user can add an account`() {
        val account = AccountModel()
        Observable.just(account.toAddIntent())
                .toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` listOf(account)
                }
    }

    @Test
    fun `user can add two accounts`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(account1.toAddIntent(), account2.toAddIntent())
                .toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` listOf(account1, account2)
                }
    }
}

class UserModelRemoveAccountIntentTests {

    @Test
    fun `user can remove only account`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` emptyList()
                }
    }

    @Test
    fun `user can remove only account after rename`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account.toRenameIntent("Account 1"),
                account.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` emptyList()
                }
    }

    @Test
    fun `user can't remove account not added`() {
        val account = AccountModel()
        Observable.just(
                account.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error!!.errorMessage `should equal` "Cannot find account " + account.id
                }
    }

    @Test
    fun `user can remove first of two accounts`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account1.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` listOf(account2)
                }
    }

    @Test
    fun `user can remove second of two accounts`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account2.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts `should equal` listOf(account1)
                }
    }
}

class AccountModelRenameUserIntentTests {

    @Test
    fun `user can rename account in user model`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account.toRenameIntent("My Account")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts.single().name `should equal` "My Account"
                }
    }

    @Test
    fun `user can rename twos account in the user model`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account1.toRenameIntent("My Account 1"),
                account2.toRenameIntent("My Account 2")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts.size `should equal` 2
                    it.userModel.accounts[0].name `should equal` "My Account 1"
                    it.userModel.accounts[1].name `should equal` "My Account 2"
                }
    }

    @Test
    fun `user can rename twos account in the user model in reverse order`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account2.toRenameIntent("My Account 2"),
                account1.toRenameIntent("My Account 1")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts.size `should equal` 2
                    it.userModel.accounts[0].name `should equal` "My Account 1"
                    it.userModel.accounts[1].name `should equal` "My Account 2"
                }
    }

    @Test
    fun `user can rename one account twice`() {
        val account1 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account1.toRenameIntent("My Account A"),
                account1.toRenameIntent("My Account B")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts.single().name `should equal` "My Account B"
                }
    }

    @Test
    fun `user cant rename account not in list yet`() {
        val account1 = AccountModel()
        Observable.just(
                account1.toRenameIntent("My Account A"),
                account1.toAddIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.userModel.accounts.single().name `should equal` ""
                }
    }

    @Test
    fun `rename of missing account results in error`() {
        val account = AccountModel()
        Observable.just(
                account.toRenameIntent("My Account A")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error!!.errorMessage `should equal` "Cannot find account " + account.id
                }
    }

    @Test
    fun `second error replaces first`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toRenameIntent("My Account A"),
                account2.toRenameIntent("My Account A")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error!!.errorMessage `should equal` "Cannot find account " + account2.id
                }
    }

    @Test
    fun `error is not automatically dismissed by another action`() {
        val account = AccountModel()
        Observable.just(
                account.toRenameIntent("My Account A"),
                account.toAddIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error `should not be` null
                }
    }

    @Test
    fun `error should keep the previous state`() {
        val account = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account2.toRenameIntent("My Account A")
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error `should not be` null
                }
                .assertWithLastTwoElements { penultimate, ultimate ->
                    penultimate.userModel `should be` ultimate.userModel
                }
    }

    @Test
    fun `user can dismiss error`() {
        val account = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account2.toRenameIntent("My Account A"),
                dismissError()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.error `should be` null
                }
                .assertWithLastAndThirdToLastElements { lastMinusTwo, last ->
                    lastMinusTwo.userModel `should be` last.userModel
                }
    }

}

class UserModelUndo {

    @Test
    fun `user gets an undo message`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account.toRenameIntent("My Account A"),
                account.toRemoveIntent()
        ).toUserModelStream()
                .assertWithLastElement {
                    it.undoModel!!.message `should equal` "Removed account 'My Account A'"
                }
    }

    @Test
    fun `user can undo a remove`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                account.toRenameIntent("My Account A"),
                account.toRemoveIntent(),
                undo()
        ).toUserModelStream()
                .assertWithLastAndThirdToLastElements { lastMinusTwo, last ->
                    lastMinusTwo.userModel.accounts `should equal` last.userModel.accounts
                }
    }

    @Test
    fun `an undone remove is in the same position it was before (index 0)`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account1.toRemoveIntent(),
                undo()
        ).toUserModelStream()
                .assertWithLastAndThirdToLastElements { lastMinusTwo, last ->
                    lastMinusTwo.userModel.accounts `should equal` last.userModel.accounts
                }
    }

    @Test
    fun `an undone remove is in the same position it was before (index 1)`() {
        val account1 = AccountModel()
        val account2 = AccountModel()
        val account3 = AccountModel()
        Observable.just(
                account1.toAddIntent(),
                account2.toAddIntent(),
                account3.toAddIntent(),
                account2.toRemoveIntent(),
                undo()
        ).toUserModelStream()
                .assertWithLastAndThirdToLastElements { lastMinusTwo, last ->
                    lastMinusTwo.userModel.accounts `should equal` last.userModel.accounts
                }
    }

    @Test
    fun `if there is nothing to undo, undo does nothing`() {
        val account = AccountModel()
        Observable.just(
                account.toAddIntent(),
                undo()
        ).toUserModelStream()
                .assertWithLastTwoElements { penultimate, ultimate ->
                    penultimate `should be` ultimate
                }
    }
}

private fun undo(): UserModelViewStateIntent = UserModelViewStateIntent.Undo()