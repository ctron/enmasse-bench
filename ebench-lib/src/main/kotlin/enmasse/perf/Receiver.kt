package enmasse.perf

import org.apache.qpid.proton.amqp.UnsignedInteger
import org.apache.qpid.proton.amqp.messaging.Accepted
import org.apache.qpid.proton.amqp.messaging.TerminusDurability
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.engine.Receiver
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.util.concurrent.atomic.AtomicLong

/**
 * @author lulf
 */
class Receiver(val address: String, msgSize: Int): BaseHandler() {

    val buffer = ByteArray(msgSize)

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
        source.timeout = UnsignedInteger(0)
        source.durable = TerminusDurability.NONE
        recv.source = source

        val target = org.apache.qpid.proton.amqp.messaging.Target()
        target.address = address
        target.timeout = UnsignedInteger(0)
        target.durable = TerminusDurability.NONE
        recv.target = target

        conn.open()
        session.open()
        recv.open()
    }

    override fun onConnectionRemoteOpen(e: Event) {
        println("Receiver connected to router ${e.connection.remoteContainer}")
    }

    override fun onTransportError(e: Event) {
        println("Transport error: ${e.transport.condition.description}")
    }

    override fun onDelivery(event: Event) {
        val recv = event.link as org.apache.qpid.proton.engine.Receiver
        val delivery = recv.current()
        //println("Got delivery: ${delivery}")
        if (delivery != null && delivery.isReadable && !delivery.isPartial) {
            val size = delivery.pending()
            val read = recv.recv(buffer, 0, buffer.size)

            recv.advance()
            delivery.disposition(Accepted())
            delivery.settle()
        }
    }
}