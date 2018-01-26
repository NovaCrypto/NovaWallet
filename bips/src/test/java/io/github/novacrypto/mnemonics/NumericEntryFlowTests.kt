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

import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.junit.Test

class EntryFlowTests {

    @Test
    fun `initial state`() {
        givenInputSequence("")
                .assertValue { m -> everyButtonIsAvailable(m) }
                .assertValue { m -> m `key is` "" }
                .assertValue { m -> m.exactMatches == emptyList<String>() }
                .assertValue { m -> m.mnemonic == emptyList<String>() }
    }

    @Test
    fun `backspace not available in initial state`() {
        givenInputSequence("")
                .assertValue { m -> !m.isBackSpaceAvailable }
    }

    @Test
    fun `backspace from initial state`() {
        givenInputSequence("<")
                .assertValue { m -> everyButtonIsAvailable(m) }
                .assertValue { m -> m `key is` "" }
                .assertValue { m -> m.exactMatches == emptyList<String>() }
    }

    @Test
    fun `on press a key`() {
        givenInputSequence("2")
                .assertValue { m -> everyButtonIsAvailable(m) }
                .assertValue { m -> m.isBackSpaceAvailable }
                .assertValue { m -> m `key is` "2" }
                .assertValue { m -> m.exactMatches == emptyList<String>() }
    }

    @Test
    fun `backspace from single key`() {
        givenInputSequence("2<")
                .assertValue { m -> everyButtonIsAvailable(m) }
                .assertValue { m -> !m.isBackSpaceAvailable }
                .assertValue { m -> m `key is` "" }
                .assertValue { m -> m.exactMatches == emptyList<String>() }
    }

    @Test
    fun `on press some keys`() {
        givenInputSequence("234")
                .assertValue { m -> m.available `should equal` setOf(2, 4, 5);true }
                .assertValue { m -> m `key is` "234" }
                .assertValue { m -> m.exactMatches == emptyList<String>() }
    }

    @Test
    fun `when value is unavailable it is ignored`() {
        givenInputSequence("2346")
                .assertValue { m -> m `key is` "234" }
    }

    @Test
    fun `on enter a word - full sequence, partial of another sequence`() {
        givenInputSequence("9463")
                .assertValue { m -> m `single word is` "wine" }
    }

    @Test
    fun `on enter a word - partial sequence`() {
        givenInputSequence("94636")
                .assertValue { m -> m.available `should equal` setOf(9);true }
                .assertValue { m -> m `single word is` "window" }
    }

    @Test
    fun `on enter a word - full sequence`() {
        givenInputSequence("946369")
                .assertValue { m -> noNumericButtonIsAvailable(m) }
                .assertValue { m -> m `single word is` "window" }
    }

    @Test
    fun `backspace window to wine`() {
        givenInputSequence("94636<")
                .assertValue { m -> m `key is` "9463" }
                .assertValue { m -> m `single word is` "wine" }
    }

    @Test
    fun `shows exact matches`() {
        givenInputSequence("227")
                .assertValue { m -> m.exactMatches `should equal` listOf("bar", "car"); true }
    }

    @Test
    fun `bottom and cotton available after 4`() {
        givenInputSequence("2688")
                .assertValue { m -> m.exactMatches == listOf("bottom", "cotton") }
    }

    @Test
    fun `3 words can be shown available after 2226`() {
        givenInputSequence("2226")
                .assertValue { m -> m.exactMatches == listOf("abandon", "account", "bacon") }
    }

    @Test
    fun `4 words not shown available after 2268`() {
        givenInputSequence("2268")
                .assertValue { m -> m.exactMatches == emptyList<String>() }
    }

}

class EntryFlowDisplayTests {
    @Test
    fun `display initially blank`() {
        givenInputSequence("")
                .assertValue { m -> m.display == "" }
    }

    @Test
    fun `display after 1 char`() {
        givenInputSequence("3")
                .assertValue { m -> m.display == "*" }
    }

    @Test
    fun `display after 483`() {
        givenInputSequence("483")
                .assertValue { m -> m.display == "**e" }
    }

    @Test
    fun `display blank after accept`() {
        givenInputSequence("2226a")
                .assertValue { m -> m.display == "" }
    }
}

class EntryFlowWordAcceptanceTests {

    @Test
    fun `On accept a word, saved words includes word`() {
        givenInputSequence("2226a")
                .assertValue { m -> m.mnemonic == listOf("abandon") }
    }

    @Test
    fun `On accept a word, backspace is not allowed and key is empty, all values available again`() {
        givenInputSequence("2226a")
                .assertValue { m -> m.isBackSpaceAvailable }
                .assertValue { m -> m.exactMatches.isEmpty() }
                .assertValue { m -> m `key is` "" }
                .assertValue { m -> everyButtonIsAvailable(m) }
    }

    @Test
    fun `on backpress restore previous state`() {
        givenInputSequence("2226a<")
                .assertValue { m -> m.exactMatches == listOf("abandon", "account", "bacon") }
                .assertValue { m -> m `key is` "2226" }
    }

    @Test
    fun `On accept two words, saved words both word`() {
        givenInputSequence("2226a,652a")
                .assertValue { m -> m.mnemonic `should equal` listOf("abandon", "okay"); true }
    }

    @Test
    fun `On accept three words, using all options`() {
        givenInputSequence("6874b,887c,887a")
                .assertValue { m -> m.mnemonic `should equal` listOf("music", "turtle", "turkey"); true }
    }
}

private fun givenInputSequence(sequence: String) =
        EntryFlow(keySequence(sequence))
                .modelStream()
                .lastElement()
                .test()

private fun keySequence(s: String): Observable<NumericEntryEvent> =
        Observable.fromIterable(s.asIterable())
                .filter { it != ',' }
                .map {
                    when (it) {
                        in '2'..'9' -> NumericEntryNumberEvent(it - '0')
                        in 'a'..'c' -> NumericEntryAcceptEvent(it - 'a')
                        '<' -> NumericEntryBackspaceEvent()
                        else -> throw Exception("Test case error, no map for $it")
                    }
                }

private fun everyButtonIsAvailable(m: NumericEntryModel) =
        (2..9).all { m.isAvailable(it) }

private fun noNumericButtonIsAvailable(m: NumericEntryModel) =
        (2..9).all { !m.isAvailable(it) }


private infix fun NumericEntryModel.`key is`(s: String) = this.currentKey == s

private infix fun NumericEntryModel.`single word is`(s: String): Boolean {
    this.exactMatches `should equal` listOf(s)
    return this.exactMatches == listOf(s)
}