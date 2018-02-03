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

import io.github.novacrypto.incubator.electrum.Electrum
import io.github.novacrypto.incubator.electrum.StratumSocket
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import kotlin.concurrent.thread

class SocketThread {

    lateinit var electrum: Electrum

    init {
        thread {
            val socket = StratumSocket.open("testnetnode.arihanc.com", 51001)
            socket.use { s ->
                s.sendRx("server.version", "2.9.2", "0.10")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { it -> Timber.d("Server says: %s", it) }

                electrum = Electrum(socket)

                electrum.balanceOf("mywkxM1Ck5SgaBjyFNE4CGvCj317CZA5Ff")
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe { it -> Timber.d("Balance is: %s", it) }

                while (true) {
                    Thread.sleep(1000)
                }
            }
        }
    }
}