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
import io.github.novacrypto.bip39.wordlists.French
import io.github.novacrypto.bip39.wordlists.Spanish
import org.amshove.kluent.`should be less or equal to`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should equal`
import org.junit.Test

class NumericTreeTests {

    @Test
    fun `root word count`() {
        val tree = givenEnglishTree()
        tree.wordCount `should equal` 2048
    }

    @Test
    fun `single level navigation limits available words`() {
        val subTree = givenEnglishTree().find("2")
        subTree.wordCount `should equal` 439
    }

    @Test
    fun `two level navigation limits available words`() {
        val subTree = givenEnglishTree().find("23")
        subTree.wordCount `should equal` 40
    }

    @Test
    fun `three level navigation limits available words`() {
        val subTree = givenEnglishTree().find("235")
        subTree.wordCount `should equal` 5
    }

    @Test
    fun `four level navigation limits available words`() {
        val subTree = givenEnglishTree().find("2356")
        subTree.wordCount `should equal` 1
        subTree[0] `should equal` "below"
    }

    @Test
    fun `direct find - witness`() {
        givenEnglishTree().find("948") `should contain only` "witness"
    }

    @Test
    fun `direct find - window`() {
        givenEnglishTree().find("946369") `should contain only` "window"
    }

    @Test
    fun `direct find - English - all words`() {
        findAllWordsAndNoMoreThan3IdenticalSequences(English.INSTANCE)
    }

    @Test
    fun `direct find - French - all words`() {
        findAllWordsAndNoMoreThan3IdenticalSequences(French.INSTANCE)
    }

    @Test
    fun `direct find - Spanish - all words`() {
        findAllWordsAndNoMoreThan3IdenticalSequences(Spanish.INSTANCE)
    }

    private fun findAllWordsAndNoMoreThan3IdenticalSequences(wordList: WordList) {
        val root = wordList.toNumericTree()
        for (word in wordList.toIterable()) {
            val key = numberize(word)
            val node = root.find(key)
            node.words `should contain` word
            node.exactMatches `should contain` word
            node.exactMatches.size `should be less or equal to` 3
        }
    }
}

class CommonPatternTests {

    @Test
    fun `common pattern for root`() {
        val root = givenEnglishTree()
        root.commonPattern `should equal` ""
    }

    @Test
    fun `common pattern for 526`() {
        val subTree = givenEnglishTree().find("526")
        subTree.commonPattern `should equal` "?a?"
    }

    @Test
    fun `common pattern for 925`() {
        val subTree = givenEnglishTree().find("925")
        subTree.commonPattern `should equal` "wal"
    }
}

private fun givenEnglishTree() = English.INSTANCE.toNumericTree()

private infix fun NumericTree.`should contain only`(justThis: String) {
    this.wordCount `should equal` 1
    this[0] `should equal` justThis
}