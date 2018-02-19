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

package io.github.novacrypto.encryption

import android.os.Build
import android.security.keystore.KeyProperties
import android.support.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class AesKeyProperties(
        val keyName: String,
        val blockMode: String = KeyProperties.BLOCK_MODE_CBC,
        val padding: String = KeyProperties.ENCRYPTION_PADDING_PKCS7
) {
    val algorithm = KeyProperties.KEY_ALGORITHM_AES
    val transform: String = "$algorithm/$blockMode/$padding"
}