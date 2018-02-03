package io.github.novacrypto.incubator.mvi

import io.github.novacrypto.incubator.electrum.Electrum
import io.reactivex.Observable

typealias WalletViewStateReducer = (WalletViewState) -> WalletViewState

typealias BalanceFactory = (String) -> Observable<Electrum.Balance>

private class WalletModel(
        intent: Observable<WalletIntent>,
        val balanceFactory: BalanceFactory
) {
    val reducers: Observable<WalletViewStateReducer> =
            intent.flatMap {
                getStateReducer(it)
            }

    private fun getStateReducer(intent: WalletIntent): Observable<WalletViewStateReducer> =
            when (intent) {
                is WalletIntent.New -> {
                    Observable.just(getInitialStateReducer())
                }
                is WalletIntent.NewBlockHeight -> Observable.just({ state -> state.copy(blockHeight = intent.height) })
                is WalletIntent.AddAddress -> getAddAddressStateReducer(intent)
                is WalletIntent.RemoveAddress -> getRemoveAddressStateReducer(intent)
            }

    private fun getAddAddressStateReducer(intent: WalletIntent.AddAddress): Observable<WalletViewStateReducer> {
        val value: WalletViewStateReducer = { it ->
            upsertAddressReducer(it, AddressViewState(intent.address, Electrum.Balance(intent.address)))
        }

        val maped: Observable<WalletViewStateReducer> =
                balanceFactory(intent.address)
                        .map { AddressViewState(intent.address, it) }
                        .map { it -> { wallet: WalletViewState -> updateAddressReducer(wallet, it) } }

        return Observable.just(value)
                .concatWith(maped)
    }

    private fun getRemoveAddressStateReducer(intent: WalletIntent.RemoveAddress): Observable<WalletViewStateReducer> {
        val value: WalletViewStateReducer = { it ->
            removeAddressReducer(it, intent.address)
        }
        return Observable.just(value)
    }

    private fun upsertAddressReducer(wallet: WalletViewState, newAddressValue: AddressViewState): WalletViewState {
        val copy = wallet.addresses.toMutableList()

        val idx = copy.indexOfFirst { (address) -> address == newAddressValue.address }
        if (idx > -1)
            copy[idx] = newAddressValue
        else
            copy += newAddressValue

        return wallet.copy(addresses = copy)
    }

    private fun updateAddressReducer(wallet: WalletViewState, newAddressValue: AddressViewState): WalletViewState {
        val idx = wallet.addresses.indexOfFirst { (address) -> address == newAddressValue.address }
        return if (idx > -1) {
            val copy = wallet.addresses.toMutableList()
                    .also { it[idx] = newAddressValue }
            wallet.copy(addresses = copy)
        } else {
            wallet
        }
    }

    private fun removeAddressReducer(wallet: WalletViewState, newAddressValue: String): WalletViewState {
        val copy = wallet.addresses.toMutableList()
        copy.removeIf { (address) -> address == newAddressValue }
        return wallet.copy(addresses = copy)
    }

    private fun getInitialStateReducer(): WalletViewStateReducer = { WalletViewState() }

    val stream: Observable<WalletViewState> = reducers.scan(
            WalletViewState(0, emptyList()),
            { oldState, reducer ->
                reducer(oldState).also {
                    println("\toldState:\t$oldState")
                    println("\tnewState:\t$it")
                }
            }
    )
}

fun walletDialog(intent: Observable<WalletIntent>, balanceFactory: BalanceFactory): Observable<WalletViewState> {
    return WalletModel(intent, balanceFactory).stream
}

