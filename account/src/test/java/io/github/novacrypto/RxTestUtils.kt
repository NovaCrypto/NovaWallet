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

package io.github.novacrypto

import io.reactivex.Observable
import org.amshove.kluent.`should be`

fun <T> Observable<T>.assertWithLastElement(function: (T) -> Unit): Observable<T> {
    this.lastOrError()
            .test()
            .assertValue {
                function(it)
                true
            }
    return this
}

fun <T> Observable<T>.assertWithLastTwoElements(
        function: (penultimate: T, ultimate: T) -> Unit
) {
    this.takeLast(2)
            .test()
            .values()
            .also {
                it.size `should be` 2
                function(it[0], it[1])
            }
}

fun <T> Observable<T>.assertWithLastAndThirdToLastElements(
        function: (lastMinusTwo: T, last: T) -> Unit
) {
    this.takeLast(3)
            .test()
            .values()
            .also {
                it.size `should be` 3
                function(it[0], it[2])
            }
}
