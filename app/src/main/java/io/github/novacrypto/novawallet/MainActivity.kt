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
import io.github.novacrypto.novawallet.uielements.Fab
import io.github.novacrypto.novawallet.uielements.MaterialSheetFabAnimator
import io.github.novacrypto.qrscanner.ScanQrActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_menu_card.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var materialSheetFab: MaterialSheetFabAnimator<Fab>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

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
                startActivityForResult(Intent(this, ScanQrActivity::class.java).apply {
                    putExtra(ScanQrActivity.OPTION_SHOW_BARCODE_BOX, BuildConfig.DEBUG)
                }, 1)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            Timber.d("Activity got barcode back [%s]", data?.extras?.getString(ScanQrActivity.BARCODE_DATA))
        }
    }
}
