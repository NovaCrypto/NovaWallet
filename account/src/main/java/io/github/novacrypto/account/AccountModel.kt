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

import io.reactivex.Observable
import java.util.*

data class AccountId(val uuid: UUID = UUID.randomUUID())

data class AccountModel(
        val id: AccountId = AccountId(),
        val name: String = ""
)

fun <T : AccountModelIntent> Observable<T>.toAccountModelStream(): Observable<AccountModel> {
    return this.scan(AccountModel(),
            { model: AccountModel, intent: AccountModelIntent ->
                accountModelReducer(intent, model)
            })
}

fun accountModelReducer(intent: AccountModelIntent, model: AccountModel): AccountModel {
    return when (intent) {
        is AccountModelIntent.Rename -> model.copy(name = intent.name)
    }
}
