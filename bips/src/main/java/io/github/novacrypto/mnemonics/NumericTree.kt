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
import java.text.Normalizer

internal fun WordList.toNumericTree() =
        NumericTree()
                .also {
                    for (word in toIterable()) it.save(word)
                }

internal class NumericTree(private val depth: Int = 0) {
    private val childNodes = mutableMapOf<Char, NumericTree>()
    val words = mutableListOf<String>()
    val exactMatches = mutableListOf<String>()
    val top3 get() = if (words.size <= 3) words else exactMatches

    val wordCount get() = words.size

    fun find(key: String): NumericTree {
        if (key.isEmpty()) return this
        return child(key[0]).find(key.substring(1))
    }

    fun save(word: String) {
        internalSave(word.toKeypadDigits(), word)
    }

    private fun internalSave(key: String, wholeWord: String) {
        words.add(wholeWord)
        commonPattern = evaluateCommon(words, depth)
        if (key.isEmpty()) {
            exactMatches.add(wholeWord)
            return
        }
        child(key[0]).internalSave(key.substring(1), wholeWord)
    }

    private fun evaluateCommon(words: List<String>, depth: Int): String {
        val firstWord = words.first()
        val sb = StringBuilder()
        for (d in 0 until depth) {
            val c = firstWord[d]
            val matched = (1 until words.size).all { words[it][d] == c }
            sb.append(if (matched) c else '?')
        }
        return sb.toString()
    }

    private fun child(c: Char) = childNodes.getOrPut(c, { NumericTree(depth + 1) })

    operator fun get(i: Int) = words[i]

    fun isAvailable(c: Char) = childNodes.containsKey(c)

    var commonPattern = ""
        private set
}

internal fun String.toKeypadDigits(): String {
    val wordNormalized = Normalizer.normalize(this, Normalizer.Form.NFD)
    val sb = StringBuilder()
    for (c in wordNormalized.toCharArray()) {
        when (c) {
            in 'a'..'c' -> sb.append('2')
            in 'd'..'f' -> sb.append('3')
            in 'g'..'i' -> sb.append('4')
            in 'j'..'l' -> sb.append('5')
            in 'm'..'o' -> sb.append('6')
            in 'p'..'s' -> sb.append('7')
            in 't'..'v' -> sb.append('8')
            in 'w'..'z' -> sb.append('9')
        }
    }
    return sb.toString()
}