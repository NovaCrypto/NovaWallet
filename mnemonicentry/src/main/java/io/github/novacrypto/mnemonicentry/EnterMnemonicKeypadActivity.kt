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

package io.github.novacrypto.mnemonicentry

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.View.*
import android.view.WindowManager
import android.widget.Button
import com.jakewharton.rxbinding2.view.clicks
import io.github.novacrypto.mnemonics.EntryFlow
import io.github.novacrypto.mnemonics.NumericEntryEvent
import io.github.novacrypto.mnemonics.NumericEntryModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_enter_mnemonic.*
import kotlinx.android.synthetic.main.fragment_enter_mnemonic.*
import kotlinx.android.synthetic.main.keypad_en.*

class EnterMnemonicKeypadActivity : AppCompatActivity() {

    companion object {
        private const val MODE = "MODE_ROOT_XPRV"
        private const val MODE_ROOT_XPRV = "MODE_ROOT_XPRV"

        const val RESULT_XPRV = "RESULT_XPRV"

        /**
         * Get an intent that will return on success the value of the entered mnemonic as the root
         * Xprv.
         */
        fun intentForGettingXprv(context: Context) =
                Intent(context, EnterMnemonicKeypadActivity::class.java).apply {
                    putExtra(MODE, MODE_ROOT_XPRV)
                }
    }

    private var dispose = CompositeDisposable()

    class NumericButton(val view: View, val number: Int)

    private lateinit var buttons2to9: List<NumericButton>

    private lateinit var putModelInResult: (NumericEntryModel, Intent) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enter_mnemonic)
        setSupportActionBar(toolbar)

        if (!BuildConfig.DEBUG) {
            preventScreenshots()
        }

        setResultMode(intent.getStringExtra(MODE))

        buttons2to9 = listOf(button2, button3, button4, button5, button6, button7, button8, button9)
                .mapIndexed { i, v -> NumericButton(v, i + 2) }

        val acceptClicks = Observable.merge(listOf(suggestionButton1, suggestionButton2, suggestionButton3)
                .mapIndexed { i, b -> b.clicks().map { _ -> NumericEntryEvent.AcceptWord(i) } })

        val numericButtonClickEvents = Observable.merge(buttons2to9
                .map { b ->
                    b.view.clicks().map { _ -> NumericEntryEvent.KeyPress(b.number) }
                })

        val backspaceEvents = buttonBackSpace.clicks().map { _ -> NumericEntryEvent.Backspace() }

        val userInput = Observable.merge(backspaceEvents, numericButtonClickEvents, acceptClicks)

        dispose.add(EntryFlow(userInput)
                .modelStream()
                .subscribe { model -> update(model) })
    }

    private fun setResultMode(extraModeString: String?) {
        putModelInResult =
                when (extraModeString) {
                    MODE_ROOT_XPRV -> { m, intent -> intent.putExtra(RESULT_XPRV, m.rootXprv) }
                    else -> { _, _ -> }
                }
    }

    private fun preventScreenshots() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onDestroy() {
        dispose.clear()
        super.onDestroy()
    }

    private fun update(model: NumericEntryModel) {
        buttons2to9.forEach { b -> b.view.isEnabled = model.available.contains(b.number) }
        buttonBackSpace.isEnabled = model.isBackSpaceAvailable
        key.text = model.display
        words.text = model.mnemonic.joinToString(" ")
        setSuggestionButton(suggestionButton1, model, 0)
        setSuggestionButton(suggestionButton2, model, 1)
        setSuggestionButton(suggestionButton3, model, 2)
        if (model.bip39MnemonicError == null) {
            valid_mnemonic_animation.visibility = VISIBLE
            valid_mnemonic_animation.playAnimation()
            valid_mnemonic_animation.setOnClickListener { finishWithResult(model) }
        } else {
            valid_mnemonic_animation.visibility = INVISIBLE
            valid_mnemonic_animation.pauseAnimation()
            valid_mnemonic_animation.setOnClickListener(null)
        }
    }

    private fun finishWithResult(resultModel: NumericEntryModel) {
        setResult(RESULT_OK, Intent().apply {
            putModelInResult(resultModel, this)
        })
        finish()
    }

    private fun setSuggestionButton(suggestionButton: Button, model: NumericEntryModel, index: Int) {
        val valid = index < model.exactMatches.size
        suggestionButton.visibility = if (valid) VISIBLE else GONE
        if (valid) suggestionButton.text = model.exactMatches[index]
    }
}