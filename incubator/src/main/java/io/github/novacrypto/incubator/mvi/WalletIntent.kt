package io.github.novacrypto.incubator.mvi

sealed class WalletIntent {
    class New : WalletIntent()
    class NewBlockHeight(val height: Int) : WalletIntent()
    class AddAddress(val address: String) : WalletIntent()
    class RemoveAddress(val address: String) : WalletIntent()
}