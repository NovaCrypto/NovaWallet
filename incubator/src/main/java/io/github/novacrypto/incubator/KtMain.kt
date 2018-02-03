import io.github.novacrypto.bip32.ExtendedPublicKey
import io.github.novacrypto.bip32.Network
import io.github.novacrypto.bip32.derivation.Derive
import io.github.novacrypto.bip32.networks.Bitcoin
import io.github.novacrypto.bip32.networks.Litecoin
import io.github.novacrypto.bip32.networks.NetworkCollection
import io.github.novacrypto.bip44.AddressIndex
import io.github.novacrypto.bip44.BIP44
import io.github.novacrypto.incubator.electrum.Electrum
import io.github.novacrypto.incubator.electrum.StratumSocket
import io.github.novacrypto.incubator.mvi.WalletIntent
import io.github.novacrypto.incubator.mvi.WalletViewState
import io.github.novacrypto.incubator.mvi.walletDialog
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.BufferedReader
import java.io.InputStreamReader

class KtMain {
    fun go() {
        //val socket = StratumSocket.open("us01.hamster.science", 50001)
        //ltc main net
        //val socket = StratumSocket.open("electrum-ltc.petrkr.net", 60001)

        // Bitcoin test net:
        val s = StratumSocket.open("testnetnode.arihanc.com", 51001)

        s.use { socket ->
            socket.sendRx("server.version", "2.9.2", "0.10")
                    .subscribe { it -> println(it) }

            val electrum = Electrum(socket)

            val userIntents = PublishSubject.create<WalletIntent>()
            val blockHeights: Observable<WalletIntent> = electrum.blockHeight()
                    .map { WalletIntent.NewBlockHeight(it) }

            val intents = blockHeights.mergeWith(userIntents)

            val walletDialog: Observable<WalletViewState> = walletDialog(intents, electrum::balanceOf)
            val subscribe = walletDialog
                    .subscribe { state -> println("Update: At blockheight ${state.blockHeight} totals: ${state.balanceConfirmed.format()} confirmed + ${state.balanceUnconfirmed.format()} unconfirmed") }

            val stdIn = BufferedReader(InputStreamReader(System.`in`))
            var userInput: String
            while (true) {
                userInput = stdIn.readLine()
                if (userInput == "exit") {
                    print("exiting...")
                    subscribe.dispose()
                    break
                } else {
                    if (userInput.startsWith("+acc")) {
                        val trimmed = userInput.substring(4).trim()
                        try {
                            val accountKey = ExtendedPublicKey.deserializer(networkCollection).deserialize(trimmed).deriveWithCache()
                            for (i in 0..20) {
                                val childKey = accountKey address i
                                userIntents.onNext(WalletIntent.AddAddress(getAppropriateAddress(childKey)))
                                val changeKey = accountKey change i
                                userIntents.onNext(WalletIntent.AddAddress(getAppropriateAddress(changeKey)))
                            }
                        } catch (e: Exception) {
                            println(e.message)
                        }
                        userIntents.onNext(WalletIntent.AddAddress(trimmed))
                    } else if (userInput.startsWith("+")) {
                        userIntents.onNext(WalletIntent.AddAddress(userInput.substring(1).trim()))
                    } else if (userInput.startsWith("-")) {
                        userIntents.onNext(WalletIntent.RemoveAddress(userInput.substring(1).trim()))
                    } else if (userInput == "new") {
                        userIntents.onNext(WalletIntent.New())
                    }
                }
            }
            println("exited")
        }
    }

    private fun getAppropriateAddress(childKey: ExtendedPublicKey) =
            if (isBip49Network(childKey))
                childKey.p2shAddress()
            else
                childKey.p2pkhAddress()

    private fun isBip49Network(childKey: ExtendedPublicKey) =
            childKey.network() in listOf(BitcoinBip49, LitecoinBip49)

    private val BitcoinBip49: Network =
            object : Network {
                override fun p2pkhVersion() = Bitcoin.MAIN_NET.p2pkhVersion()
                override fun p2shVersion() = Bitcoin.MAIN_NET.p2shVersion()
                override fun getPublicVersion() = 0x49d7cb2
                override fun getPrivateVersion() = Bitcoin.MAIN_NET.privateVersion
            }

    private val LitecoinBip44: Network =
            object : Network {
                override fun p2pkhVersion() = Litecoin.MAIN_NET.p2pkhVersion()
                override fun p2shVersion() = Litecoin.MAIN_NET.p2shVersion()
                override fun getPublicVersion() = Bitcoin.MAIN_NET.publicVersion
                override fun getPrivateVersion() = Litecoin.MAIN_NET.privateVersion
            }

    private val LitecoinBip49: Network =
            object : Network {
                override fun p2pkhVersion() = Litecoin.MAIN_NET.p2pkhVersion()
                override fun p2shVersion() = Litecoin.MAIN_NET.p2shVersion()
                override fun getPublicVersion() = 0x1b26ef6
                override fun getPrivateVersion() = Litecoin.MAIN_NET.privateVersion
            }

    private val networkCollection: NetworkCollection = NetworkCollection(
            LitecoinBip44,
            Litecoin.MAIN_NET,
            Bitcoin.MAIN_NET,
            Bitcoin.TEST_NET,
            BitcoinBip49,
            LitecoinBip49
    )

    private fun addressIndex(i: Int) =
            BIP44.m()
                    .purpose44()
                    .coinType(0)
                    .account(0)
                    .external()
                    .address(i)

    private fun changeIndex(i: Int) =
            BIP44.m()
                    .purpose44()
                    .coinType(0)
                    .account(0)
                    .internal()
                    .address(i)

    private infix fun <Key> Derive<Key>.address(i: Int) = this.derive(addressIndex(i), AddressIndex.DERIVATION_FROM_ACCOUNT)
    private infix fun <Key> Derive<Key>.change(i: Int) = this.derive(changeIndex(i), AddressIndex.DERIVATION_FROM_ACCOUNT)

    private fun Long.format() = (this.toDouble() / 100000000.0).toString()
}
