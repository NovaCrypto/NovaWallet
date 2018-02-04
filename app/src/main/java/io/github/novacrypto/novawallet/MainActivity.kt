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

package io.github.novacrypto.novawallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.facebook.litho.ComponentContext
import com.facebook.litho.LithoView
import com.facebook.litho.sections.SectionContext
import com.facebook.litho.sections.widget.RecyclerCollectionComponent
import io.github.novacrypto.base58.Base58.base58Encode
import io.github.novacrypto.bip32.ExtendedPrivateKey
import io.github.novacrypto.bip32.ExtendedPublicKey
import io.github.novacrypto.bip32.Index.isHardened
import io.github.novacrypto.bip32.Network
import io.github.novacrypto.bip32.networks.Bitcoin
import io.github.novacrypto.bip32.networks.Litecoin
import io.github.novacrypto.bip44.Account
import io.github.novacrypto.bip44.AddressIndex
import io.github.novacrypto.bip44.BIP44
import io.github.novacrypto.bip44.BIP44.m
import io.github.novacrypto.bip44.Purpose
import io.github.novacrypto.incubator.electrum.Electrum
import io.github.novacrypto.mnemonicentry.EnterMnemonicKeypadActivity
import io.github.novacrypto.novawallet.customscanners.XPubScannerActivity
import io.github.novacrypto.novawallet.uielements.Fab
import io.github.novacrypto.novawallet.uielements.MaterialSheetFabAnimator
import io.github.novacrypto.novawallet.uilitho.ListSection
import io.github.novacrypto.qrscanner.ScanQrActivity
import io.github.novacrypto.security.AsymmetricSecurity
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_menu_card.*
import kotlinx.android.synthetic.main.content_main.*
import timber.log.Timber

private const val REQUEST_SCAN = 1
private const val REQUEST_MNEMONIC = 2

class MainActivity : AppCompatActivity() {

    companion object {
        private val security = AsymmetricSecurity()
    }

    private lateinit var materialSheetFab: MaterialSheetFabAnimator<Fab>

    private lateinit var context: ComponentContext

    private lateinit var lithoView: LithoView

    private lateinit var socketThread: SocketThread

    val electrum: Electrum
        get() = socketThread.electrum

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        socketThread = SocketThread()

        context = ComponentContext(this)

        val component =
                RecyclerCollectionComponent.create(context)
                        .disablePTR(true)
                        .section(ListSection.create(SectionContext(context))
                                .data(listOf(AddressModel(
                                        coinIcon = R.drawable.ic_coin_litecoin,
                                        path = "Click the plus",
                                        address = "")))
                                .build())
                        .build()

        lithoView = LithoView.create(context, component)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        main_area.addView(lithoView)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action") {
                        Timber.d("action!")
                    }
                    .setAction("Action 2") {
                        Timber.d("action 2!")
                    }
                    .show()
        }

        val fab = findViewById<Fab>(R.id.fab)
        val sheetView = findViewById<View>(R.id.fab_sheet)
        val overlay = findViewById<View>(R.id.dim_overlay)

        hookUpAddAccountButton()

        materialSheetFab = MaterialSheetFabAnimator(fab, sheetView, overlay, R.color.fab_sheet_color, R.color.fab_color)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (materialSheetFab.isSheetVisible) {
            materialSheetFab.hideSheet()
        } else {
            super.onBackPressed()
        }
    }

    private fun hookUpAddAccountButton() {
        add_account.setOnClickListener { _ ->
            Timber.d("Add account")
            materialSheetFab.hideSheetAndAfter {
                startActivityForResult(Intent(this, XPubScannerActivity::class.java).apply {
                    putExtra(ScanQrActivity.OPTION_SHOW_BARCODE_BOX, BuildConfig.DEBUG)
                }, REQUEST_SCAN)
            }
        }
        add_mnemonic_account.setOnClickListener { _ ->
            Timber.d("Add mnemonic account")
            materialSheetFab.hideSheetAndAfter {
                startActivityForResult(EnterMnemonicKeypadActivity.intentForGettingXprv(this, security), REQUEST_MNEMONIC)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_SCAN && resultCode == Activity.RESULT_OK) {
            val barcode = data?.extras?.getString(ScanQrActivity.BARCODE_DATA) ?: ""
            Timber.d("Activity got barcode back [%s]", barcode)
            quickDemo(barcode, BIP44.m().purpose44())
        }
        if (requestCode == REQUEST_MNEMONIC && resultCode == Activity.RESULT_OK) {
            val encoded = data?.extras?.getString(EnterMnemonicKeypadActivity.RESULT_XPRV)!!
            Timber.d("Activity got xprv back encoded [%s]", encoded)
            val xprv = security.decoder().decodeByteArray(encoded)
            Timber.d("Activity got xprv back [%s]", base58Encode(xprv))
            quickDemo(xprv)
        }
    }

    private var accounts = listOf<AddressModel>()

    private fun quickDemo(rootXrpv: ByteArray) {
        val deriver = ExtendedPrivateKey
                .deserializer()
                .deserialize(rootXrpv)
                .deriveWithCache()
        listOf<Network>(Bitcoin.TEST_NET)//Bitcoin.MAIN_NET, Bitcoin.TEST_NET, Litecoin.MAIN_NET)
                .flatMap {
                    listOf(
                            deriver.derive(m()
                                    .purpose44()
                                    .coinType(networkToBip44Coin(it))
                                    .account(0), Account.DERIVATION)
                                    .neuter()
                                    .toNetwork(it)
                                    .extendedBase58() to m().purpose44(),
                            deriver.derive(m()
                                    .purpose49()
                                    .coinType(networkToBip44Coin(it))
                                    .account(0), Account.DERIVATION)
                                    .neuter()
                                    .toNetwork(it)
                                    .extendedBase58() to m().purpose49()
                    )
                }
                .forEach { quickDemo(it.first, it.second) }
    }

    private fun quickDemo(barcode: String, purpose: Purpose) {
        try {
            val public = ExtendedPublicKey.deserializer().deserialize(barcode)
            if (public.depth() == 4) {
                throw Exception("depth must be 4")
            }
            if (!isHardened(public.childNumber())) {
                throw Exception("Account number not hardened")
            }
            val deriver = public.deriveWithCache()
            val external = purpose
                    .coinType(networkToBip44Coin(public.network()))
                    .account((public.childNumber() - 0x80000000).toInt())
                    .external()
            for (i in 0..19) {
                val addressIndex = external
                        .address(i)
                accounts += AddressModel(
                        coinIcon = coinRes(public.network()),
                        path = addressIndex.toString(),
                        address = deriver
                                .derive(addressIndex, AddressIndex.DERIVATION_FROM_ACCOUNT)
                                .run {
                                    when {
                                        purpose.value == 44 -> p2pkhAddress()
                                        purpose.value == 49 -> p2shAddress()
                                        else -> throw RuntimeException("Unknown purpose")
                                    }
                                })
            }
        } catch (e: Exception) {
            accounts += AddressModel(
                    coinIcon = R.drawable.ic_error_outline_black,
                    path = "Error",
                    address = e.message.toString())
            Timber.e(e)
        }
        updateLithoView()

        Observable.merge(
                accounts.map {
                    electrum.balanceOf(it.address)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    updateValueOfAddress(it.address, it)
                }
    }

    private fun updateLithoView() {
        val component =
                RecyclerCollectionComponent.create(context)
                        .disablePTR(true)
                        .section(ListSection.create(SectionContext(context))
                                .data(accounts).build())
                        .build()
        lithoView.setComponent(component)
    }

    private fun updateValueOfAddress(address: String, newValue: Electrum.Balance) {
        val index = accounts.indexOfFirst { it.address == address }
        val toMutableList = accounts.toMutableList()
        toMutableList[index] = accounts[index].copy(value = formatBalance(newValue))
        accounts = toMutableList
        updateLithoView()
    }

    private fun formatBalance(balance: Electrum.Balance): String {
        val div = 100000000.0
        if (balance.unusedAddress)
            return "unused"
        if (balance.unconfirmed != 0L) {
            return String.format("%.8f (%.8f)", balance.confirmed / div, balance.unconfirmed / div)
        }
        return String.format("%.8f", balance.confirmed / div)
    }

    private fun coinRes(network: Network?) =
            when (network) {
                Bitcoin.MAIN_NET -> R.drawable.ic_coin_bitcoin
                Bitcoin.TEST_NET -> R.drawable.ic_coin_bitcoin_testnet
                Litecoin.MAIN_NET -> R.drawable.ic_coin_litecoin
                else -> throw Exception("Unknown network")
            }

    private fun networkToBip44Coin(network: Network) =
            when (network) {
                Bitcoin.MAIN_NET -> 0
                Bitcoin.TEST_NET -> 1
                Litecoin.MAIN_NET -> 2
                else -> throw Exception("Unknown network")
            }
}