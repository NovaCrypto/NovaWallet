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

import io.github.novacrypto.bip39.MnemonicValidator
import io.github.novacrypto.bip39.Validation.InvalidChecksumException
import io.github.novacrypto.bip39.Validation.InvalidWordCountException
import io.github.novacrypto.bip39.WordList

internal class Validator(wordList: WordList) {

    private val validator = MnemonicValidator.ofWordList(wordList)

    fun validateMnemonic(mnemonic: List<String>): MnemonicError? {
        if (mnemonic.size < 12) return MnemonicError.WORD_COUNT
        return try {
            validator.validate(mnemonic)
            null
        } catch (e: InvalidChecksumException) {
            MnemonicError.CHECKSUM
        } catch (e: InvalidWordCountException) {
            MnemonicError.WORD_COUNT
        }
    }
}