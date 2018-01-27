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

package io.github.novacrypto.novawallet.uielements

import android.support.annotation.ColorRes
import android.view.View
import com.gordonwong.materialsheetfab.AnimatedFab
import com.gordonwong.materialsheetfab.MaterialSheetFab
import com.gordonwong.materialsheetfab.animations.AnimationListener
import io.github.novacrypto.novawallet.getColorCompat

class MaterialSheetFabAnimator<FAB>(
        view: FAB,
        sheet: View,
        overlay: View,
        @ColorRes sheetColorId: Int,
        @ColorRes fabColorId: Int
) : MaterialSheetFab<FAB>(
        view,
        sheet,
        overlay,
        view.resources.getColorCompat(sheetColorId),
        view.resources.getColorCompat(fabColorId)
) where FAB : View, FAB : AnimatedFab {

    fun hideSheetAndAfter(function: () -> Unit) {
        if (!isSheetVisible) {
            function()
        } else {
            hideSheet(object : AnimationListener() {
                override fun onEnd() {
                    function()
                }
            })
        }
    }
}
