package io.github.novacrypto.incubator.mvi

import io.github.novacrypto.incubator.electrum.Electrum

data class WalletViewState(
        val blockHeight: Int = 0,
        val addresses: List<AddressViewState> = emptyList()) {
    val balanceConfirmed: Long = addresses.sumByDouble { it.balance.confirmed.toDouble() }.toLong()
    val balanceUnconfirmed: Long = addresses.sumByDouble { it.balance.unconfirmed.toDouble() }.toLong()
}

data class AddressViewState(
        val address: String,
        val balance: Electrum.Balance
)