package enmasse.perf

import org.apache.qpid.proton.Proton
import org.apache.qpid.proton.amqp.messaging.Accepted
import org.apache.qpid.proton.amqp.transport.DeliveryState
import org.apache.qpid.proton.amqp.transport.SenderSettleMode
import org.apache.qpid.proton.engine.Event

/**
 * @author lulf
 */
class Receiver(val hostname: String, val port: Int, val address: String, msgSize: Int): ClientHandler() {
    val buffer = ByteArray(msgSize)
    public var msgsReceived = 0


    override fun onReactorInit(event: Event) {
        event.reactor.connectionToHost(hostname, port, this)
    }

    override fun onConnectionInit(event: Event) {
        println("Connection initialized")
        val conn = event.connection
        conn.container = "enmasse-bench2"
        val session = conn.session()
        val recv = session.receiver("enmasse-bench-receiver")

        val source = org.apache.qpid.proton.amqp.messaging.Source()
        source.address = address
        //source.timeout = UnsignedInteger(0)
        //source.durable = TerminusDurability.NONE
        //source.expiryPolicy = TerminusExpiryPolicy.LINK_DETACH
        recv.source = source

        conn.open()
        session.open()
        recv.open()
    }

    override fun onTransportError(e: Event) {
        println("Transport: ${e.transport.condition.description}")
    }

    override fun onDelivery(event: Event) {
        val recv = event.link as org.apache.qpid.proton.engine.Receiver
        recv.current()
        val delivery = recv.current()
        //println("Got delivery: ${delivery}")
        if (delivery != null && delivery.isReadable && !delivery.isPartial) {
            val size = delivery.pending()
            val read = recv.recv(buffer, 0, buffer.size)
            recv.advance()
            delivery.settle()
            msgsReceived++
        }
    }
}