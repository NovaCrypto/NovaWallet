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

package io.github.novacrypto.account

data class UserModel(
        val accounts: List<AccountModel> = emptyList()
)

sealed class UserModelIntent {
    class AddAccountIntent(val account: AccountModel, val index: Int? = null) : UserModelIntent()
    class RemoveAccountIntent(val accountId: AccountId) : UserModelIntent()
    class ForwardAccountModelIntent(
            val accountId: AccountId,
            val intent: AccountModelIntent
    ) : UserModelIntent()
}

fun UserModelIntent.forward() = UserModelViewStateIntent.ForwardUserModelIntent(this)
