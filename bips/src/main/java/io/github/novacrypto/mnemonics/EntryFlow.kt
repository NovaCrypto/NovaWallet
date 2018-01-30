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

package io.github.novacrypto.mnemonics

import io.github.novacrypto.bip39.WordList
import io.github.novacrypto.bip39.wordlists.English
import io.reactivex.Observable

class EntryFlow(private val input: Observable<NumericEntryEvent>) {
    private val wordList: WordList = English.INSTANCE
    private val root: NumericTree = wordList.toNumericTree()
    private val validator: Validator = Validator(wordList)

    fun modelStream(): Observable<NumericEntryModel> =
            input.scan(initialState(),
                    { model: NumericEntryModel, event: NumericEntryEvent ->
                        when (event) {
                            is NumericEntryEvent.KeyPress -> onNumberPress(model, event, root)
                            is NumericEntryEvent.Backspace -> onBackspacePress(model)
                            is NumericEntryEvent.AcceptWord -> onAcceptPress(model, event, root)
                        }
                    })

    private fun initialState() =
            NumericEntryModel(currentKey = "",
                    display = "",
                    available = root.toAvailableSet(),
                    mnemonic = emptyList(),
                    exactMatches = emptyList())

    private fun onAcceptPress(model: NumericEntryModel, event: NumericEntryEvent.AcceptWord, root: NumericTree): NumericEntryModel {
        val mnemonic = model.mnemonic + listOf(model.exactMatches[event.acceptOption])
        return NumericEntryModel(
                currentKey = "",
                mnemonic = mnemonic,
                exactMatches = emptyList(),
                available = if (mnemonic.size == 24) emptySet() else root.toAvailableSet(),
                display = "",
                previousState = model,
                bip39MnemonicError = validator.validateMnemonic(mnemonic))
    }

    private fun onBackspacePress(model: NumericEntryModel) = model.previousState ?: model

    private fun onNumberPress(model: NumericEntryModel, event: NumericEntryEvent.KeyPress, root: NumericTree): NumericEntryModel {
        return if (!model.available.contains(event.number)) {
            model
        } else {
            val key = model.currentKey + event.number
            keyToModel(root, key, model)
        }
    }

    private fun keyToModel(root: NumericTree, key: String, model: NumericEntryModel): NumericEntryModel {
        val node = root.find(key)
        return NumericEntryModel(
                currentKey = key,
                available = node.toAvailableSet(),
                display = node.commonPattern.replace('?', '*'),
                exactMatches = node.top3,
                mnemonic = model.mnemonic,
                previousState = model
        )
    }

    private fun NumericTree.toAvailableSet(): Set<Int> {
        return (2..9).filter { this.isAvailable('0' + it) }.toSet()
    }
}