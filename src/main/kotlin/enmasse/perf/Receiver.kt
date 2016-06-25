package enmasse.perf

import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker

/**
 * @author lulf
 */
class Receiver(val address: String, msgSize: Int): BaseHandler() {
    val buffer = ByteArray(msgSize)
    var msgsReceived = 0L

    init {
        add(Handshaker())
        add(FlowController())
    }

    override fun onConnectionInit(event: Event) {
        val conn = event.connection
        conn.container = "ebench-recv"
        val session = conn.session()
        val recv = session.receiver("ebench-recv")

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