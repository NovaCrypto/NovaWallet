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

import io.github.novacrypto.base58.Base58.base58Encode
import io.github.novacrypto.bip39.wordlists.English
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
                .assertValue { m -> m.exactMatches == listOf("window", "wine") }
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
                .assertValue { m -> m.exactMatches == listOf("window", "wine") }
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

    @Test
    fun `where there's an exact match and the partial matches fit, show all`() {
        givenInputSequence("4273")
                .assertValue { m -> m.exactMatches == listOf("garden", "hard") }
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

class NumericEntryValidationTests {

    @Test
    fun `initial state the error reports as incomplete`() {
        givenInputSequence("")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.INCOMPLETE }
    }

    @Test
    fun `on a couple of keys presses, the error reports as incomplete`() {
        givenInputSequence("28")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.INCOMPLETE }
    }

    @Test
    fun `valid mnemonic is valid`() {
        givenMnemonicInput("butter jump news kite cliff number good mansion mushroom virtual boil duty")
                .assertValue { m -> m.bip39MnemonicError == null }
    }

    @Test
    fun `partway though a word it shows as incomplete`() {
        givenMnemonicInput("butter jump news kite cliff number good mansion mushroom virtual boil", "388")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.INCOMPLETE }
                .assertValue { m -> m `key is` "388" }
    }

    @Test
    fun `backspacing a valid mnemonic, shows as incomplete`() {
        givenPartialMnemonicInput("butter jump news kite cliff number good mansion mushroom virtual boil duty", "<")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.INCOMPLETE }
                .assertValue { m -> m `key is` "3889" }
    }

    @Test
    fun `invalid mnemonic by way of checksum`() {
        givenMnemonicInput("butter jump news kite cliff number good mansion mushroom virtual boil boil")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.CHECKSUM }
    }

    @Test
    fun `invalid mnemonic by way of wordcount`() {
        givenMnemonicInput("butter jump news kite cliff number good mansion mushroom virtual boil")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.WORD_COUNT }
    }
}

class EntryFlowWordLimitTests {

    @Test
    fun `15 words is valid`() {
        givenMnemonicInput("never dog canyon spread captain hill desk arrest tired face strong oven jewel image reason")
                .assertValue { m -> m.bip39MnemonicError == null }
                .assertValue { m -> everyButtonIsAvailable(m) }
    }

    @Test
    fun `18 words is valid`() {
        givenMnemonicInput("lock omit clean move purse crumble history speak hint situate speed slight soccer raise decrease world board range")
                .assertValue { m -> m.bip39MnemonicError == null }
                .assertValue { m -> everyButtonIsAvailable(m) }
    }

    @Test
    fun `21 words is valid`() {
        givenMnemonicInput("illness market index jelly twice use often must fun hood hope mirror metal idle absurd silent oxygen garbage best rose curve")
                .assertValue { m -> m.bip39MnemonicError == null }
                .assertValue { m -> everyButtonIsAvailable(m) }
    }

    @Test
    fun `after 24 words, no more keys are available`() {
        givenMnemonicInput("aisle perfect crush pistol fly enable ketchup mixture usage elbow insect retire bitter essay midnight claw toe swamp gather great extend street approve coach")
                .assertValue { m -> m.bip39MnemonicError == null }
                .assertValue { m -> noNumericButtonIsAvailable(m) }
    }

    @Test
    fun `before 12 words (9), word count error shows`() {
        givenMnemonicInput("device isolate odor clinic child hotel inch regret stumble")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.WORD_COUNT }
    }

    @Test
    fun `before 12 words (6), word count error shows`() {
        givenMnemonicInput("deer direct buffalo embrace hedgehog replace")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.WORD_COUNT }
    }

    @Test
    fun `before 12 words (3), word count error shows`() {
        givenMnemonicInput("napkin help genius")
                .assertValue { m -> m.bip39MnemonicError == MnemonicError.WORD_COUNT }
    }

}

class RootXprvTests {

    @Test
    fun `12 words Root xprv`() {
        givenMnemonicInput("canvas board before salon prison expose action exist cycle hybrid simple father")
                .assertValue { m -> base58Encode(m.rootXprv) == "xprv9s21ZrQH143K4ZxFZTAyky5RFeyuboFTuEGbXzEm1DRUaeFn9chknYxeDv725BAXUMUXrRREs5jBsMY2tJEcJr5CK8135txBdSuZWXaDUS3" }
    }

    @Test
    fun `15 words Root xprv`() {
        givenMnemonicInput("motion spring copper double release cage business employ insane figure large robust cost utility mixture")
                .assertValue { m -> base58Encode(m.rootXprv) == "xprv9s21ZrQH143K2o632LMYwmdF4dBa3NyvSQpUvdAVgZ9ujR5jeTh4qUse4S6tHEaAn4Cwge6DqKn8u1teShhqSBsgNReQvB2YKEUuNZ8DFzJ" }
    }

    @Test
    fun `24 words Root xprv`() {
        givenMnemonicInput("climb wear team abandon giggle pledge vote hurt combine industry duck flee electric rifle inform neck accident flip merit material illegal bargain myth cable")
                .assertValue { m -> base58Encode(m.rootXprv) == "xprv9s21ZrQH143K45oiSkqwJiAfcD4nKscyH5yDq8HX7HwM8zvzHDPvgVdXtM28xQPF7M2s3r9JbbkeK9ntvkzegLcfy3M9vr9MXXFFhBvLFqZ" }
    }
}

private fun givenMnemonicInput(mnemonic: String, additional: String = "") =
        givenInputSequence(numberizeAndAccept(mnemonic) + additional)
                .assertValue { m -> m.mnemonic == mnemonic.split(" ") }

private fun givenPartialMnemonicInput(mnemonic: String, additional: String = "") =
        givenInputSequence(numberizeAndAccept(mnemonic) + additional)

private fun numberizeAndAccept(englishString: String): String {
    val root = English.INSTANCE.toNumericTree()
    return englishString.split(" ")
            .map { it to it.toKeypadDigits() }
            .joinToString(",") {
                it.second + ('a' + root.find(it.second).top3.indexOf(it.first))
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
                        in '2'..'9' -> NumericEntryEvent.KeyPress(it - '0')
                        in 'a'..'c' -> NumericEntryEvent.AcceptWord(it - 'a')
                        '<' -> NumericEntryEvent.Backspace()
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