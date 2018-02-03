package io.github.novacrypto.incubator.electrum

import io.reactivex.Observable
import io.reactivex.Single

class Electrum(private val socket: StratumSocket) {

    fun balanceNowOf(address: String): Single<Balance> {
        return socket.sendRx(BalanceDto::class.java, "blockchain.address.get_balance", address)
                .map { b -> Balance(address, b) }
    }

    fun balanceOf(address: String): Observable<Balance> {
        return socket.sendRx("blockchain.address.subscribe", address)
                .toObservable()
                .flatMap { s ->
                    firstBalance(s, address).toObservable().concatWith(
                            socket.addressSubscriptionsFor(address)
                                    .flatMap { balanceNowOf(address).toObservable() }
                    )
                }
    }

    private fun firstBalance(s: String, address: String): Single<Balance> {
        return if ("null" == s) {
            Single.just(Balance(address))
        } else balanceNowOf(address)
    }

    fun blockHeight(): Observable<Int> {
        return socket.sendRx(Int::class.java, "blockchain.numblocks.subscribe")
                .toObservable()
                .concatWith(socket.blockHeights)
    }

    internal class BalanceDto {
        internal var confirmed: Long = 0
        internal var unconfirmed: Long = 0
    }

    class Balance {
        val address: String
        val confirmed: Long
        val unconfirmed: Long
        private val unusedAddress: Boolean

        internal constructor(address: String, b: BalanceDto) {
            this.address = address
            confirmed = b.confirmed
            unconfirmed = b.unconfirmed
            unusedAddress = false
        }

        constructor(address: String) {
            this.address = address
            confirmed = 0
            unconfirmed = 0
            unusedAddress = true
        }

        override fun toString(): String {
            if (unusedAddress)
                return String.format("Balance of %s 0 (Unused)", address)
            return if (unconfirmed == 0L) String.format("Balance of %s %d confirmed", address, confirmed) else String.format("Balance of %s %d confirmed + %d unconfirmed", address, confirmed, unconfirmed)
        }
    }
}
