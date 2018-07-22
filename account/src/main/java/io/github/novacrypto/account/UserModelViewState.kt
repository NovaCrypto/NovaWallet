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

data class UserModelViewState(
        val userModel: UserModel = UserModel(),
        val error: ErrorModel? = null,
        val undoModel: UserModelViewStateUndoModel? = null
)

data class UserModelViewStateUndoModel(
        val message: String,
        val undoIntent: UserModelViewStateIntent
)

fun <T : UserModelViewStateIntent> Observable<T>.toUserModelStream(): Observable<UserModelViewState> {
    return this.scan(UserModelViewState(),
            { model: UserModelViewState, intent: UserModelViewStateIntent ->
                model.transformWithIntent(intent)
            })
}

private fun UserModelViewState.transformWithIntent(intent: UserModelViewStateIntent): UserModelViewState {
    return when (intent) {
        is UserModelViewStateIntent.DismissError -> copy(error = null)
        is UserModelViewStateIntent.ForwardUserModelIntent ->
            try {
                val (newModel, undoModel) = userModel.transformWithIntent(intent.userModelIntent)
                copy(userModel = newModel, undoModel = undoModel)
            } catch (e: UserReportableException) {
                copy(error = ErrorModel(e.message ?: ""))
            }
        is UserModelViewStateIntent.Undo ->
            undoModel?.undoIntent?.let {
                transformWithIntent(it
                )
            } ?: this
    }
}

private fun UserModel.transformWithIntent(intent: UserModelIntent) =
        when (intent) {
            is UserModelIntent.AddAccountIntent -> {
                copy(accounts = if (intent.index != null) {
                    accounts.insert(intent.account, intent.index)
                } else {
                    accounts + intent.account
                }).withNoUndo()
            }
            is UserModelIntent.RemoveAccountIntent -> {
                withAccountAndIndexForId(intent.accountId) { account, index ->
                    Pair(copy(accounts = accounts - account),
                            UserModelViewStateUndoModel("Removed account '${account.name}'",
                                    account.toInsertIntent(index)))
                }
            }
            is UserModelIntent.ForwardAccountModelIntent -> {
                withAccountAndIndexForId(intent.accountId) { account, index ->
                    val newAccounts = accounts.replace(index, accountModelReducer(intent.intent, account))
                    copy(accounts = newAccounts).withNoUndo()
                }
            }
        }

private fun <T> UserModel.withAccountAndIndexForId(accountId: AccountId, transform: (account: AccountModel, index: Int) -> T): T {
    val accountIdx = accounts.indexOfFirst { it.id == accountId }.also {
        if (it == -1) throw cannotFind(accountId)
    }
    val existingAccount = accounts[accountIdx]
    return transform(existingAccount, accountIdx)
}

private fun UserModel.withNoUndo() = Pair(this, null)

private inline fun <E> List<E>.copyAndModify(modify: MutableList<E>.() -> Unit): List<E> =
        toMutableList().apply(modify)

private fun <E> List<E>.insert(newItem: E, index: Int) =
        copyAndModify {
            this.add(index, newItem)
        }

private fun <E> List<E>.replace(index: Int, replacement: E) =
        copyAndModify {
            this[index] = replacement
        }

private fun cannotFind(accountId: AccountId) =
        UserReportableException("Cannot find account $accountId")
