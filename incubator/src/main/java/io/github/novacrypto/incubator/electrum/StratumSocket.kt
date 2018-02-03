package io.github.novacrypto.incubator.electrum

import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observables.ConnectableObservable
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class StratumSocket
//private final PublishSubject<Object> closeSignal = PublishSubject.create();

(private val lineReader: LineReader, private val out: PrintWriter) : AutoCloseable {
    private val rx = CompositeDisposable()
    private val results: Observable<Response>
    val messages: Observable<Invoke>
    private val messageIdx = AtomicInteger()
    private val addressChangeMessages: Observable<AddressChange>
    val blockHeights: Observable<Int>

    init {

        val from = from(lineReader)
        results = from.map { s ->
            //System.out.println("in: " + s);
            Gson().fromJson(s, Response::class.java)
        }.cache()
        //.takeUntil(closeSignal);

        messages = results.filter { m -> m.id == null }
                .map<Invoke> { m -> m }

        addressChangeMessages = messages
                .filter { m -> "blockchain.address.subscribe" == m.method }
                .map { m -> AddressChange(m.params) }

        blockHeights = messages
                .filter { m -> "blockchain.numblocks.subscribe" == m.method }
                .map<Any> { m -> m.params }
                .cast(List::class.java)
                .map { l -> l[0] }
                .cast(Double::class.java)
                .map { it.toInt() }

        rx.add(from.connect())

        startKeepAlive()
    }

    private fun startKeepAlive() {
        rx.add(
                Observable.interval(1, TimeUnit.MINUTES, Schedulers.io())
                        .subscribe { _ -> sendRx("server.version", "2.9.2", "0.10") }
        )
    }

    @Throws(Exception::class)
    override fun close() {
        print("Closing...")
        lineReader.close()
        //closeSignal.onNext(new Object());
        print("A...")
        //socket.close();
        print("B...")
        //in.close();
        print("C...")
        out.close()
        print("D...")
        rx.clear()
        println("Closed")
    }

    fun send(command: String, vararg params: String): Int {
        val messageIdx = this.messageIdx.getAndIncrement()
        send(messageIdx, command, *params)
        return messageIdx
    }

    private fun send(messageIdx: Int, command: String, vararg params: Any) {
        val messageString = formatMessage(messageIdx, command, *params)
        //System.out.println("out: " + messageString);
        out.println(messageString)
    }

    fun sendRx(command: String, vararg params: Any): Single<String> {
        val messageIdx = this.messageIdx.getAndIncrement()
        val stringSingle = Single.fromObservable(results
                .filter { r -> r.id != null && r.id == messageIdx }
                .map { r -> if (r.result == null) "null" else r.result }
                .map { r ->
                    r as? String ?: Gson().toJson(r)
                }
                .take(1))
        send(messageIdx, command, *params)
        return stringSingle
    }

    fun <T> sendRx(clazz: Class<T>, command: String, vararg params: String): Single<T> {
        return sendRx(command, *params)
                .map { r -> Gson().fromJson(r, clazz) }
    }

    fun addressSubscriptionsFor(address: String): Observable<AddressChange> {
        return addressChangeMessages.filter { addressChange -> addressChange.params!!.contains(address) }
    }

    class AddressChange(params: Any?) {
        internal var params: String? = null

        init {
            //System.out.println("New Address change " + params);
            this.params = params?.toString()
        }
    }

    class Message {
        internal var id: Int? = null
        internal var method: String? = null
        internal var params: List<Any>? = null

        override fun toString(): String {
            return Gson().toJson(this)
        }
    }

    open class Invoke {
        internal var id: Int? = null
        internal var method: String? = null
        internal var params: Any? = null
    }

    private class Response : Invoke() {
        internal var result: Any? = null
    }

    companion object {

        @Throws(IOException::class)
        fun open(host: String, port: Int): StratumSocket {
            val socket = Socket(host, port)
            val out = PrintWriter(socket.getOutputStream(), true)

            return StratumSocket(SocketLineReader(socket), out)
        }

        private fun from(reader: LineReader): ConnectableObservable<String> {
            val tObservableOnSubscribe: ObservableOnSubscribe<String> = ObservableOnSubscribe { subscriber ->
                try {
                    println("Creating a subscriber")
                    while (!subscriber.isDisposed) {
                        val line = reader.readLine() ?: break
                        System.out.println("Read with RX :" + line + " Thread " + Thread.currentThread().name)
                        subscriber.onNext(line)
                    }
                } catch (e: IOException) {
                    println("Threw " + e.javaClass)
                    subscriber.onError(e)
                }

                println("Completed")
                subscriber.onComplete()
            }
            val exit = Observable.create<String>(tObservableOnSubscribe)
                    .subscribeOn(Schedulers.io())
            return exit.publish()
        }

        private fun formatMessage(id: Int, command: String, vararg params: Any): String {
            val message = Message()
            message.id = id
            message.method = command
            message.params = listOf(*params)
            return message.toString()
        }
    }
}
