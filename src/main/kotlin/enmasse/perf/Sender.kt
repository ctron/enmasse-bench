package enmasse.perf

import org.apache.qpid.proton.Proton
import org.apache.qpid.proton.amqp.Binary
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.amqp.transport.DeliveryState
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Sender(val hostname:String, val port: Int, val address: String, val msgSize: Int, runTime: Int): ClientHandler(runTime) {

    var nextTag = 0
    var msgsSent = 0
    val msgBuffer: ByteArray = ByteArray(1024)
    var msgLen = 0

    init {
        val msg = Proton.message()
        msg.body = AmqpValue(Binary(1.rangeTo(msgSize).map { a -> a.toByte() }.toByteArray()))
        msgLen = msg.encode(msgBuffer, 0, msgBuffer.size)
    }

    override fun onReactorInit(event: Event) {
        super.onReactorInit(event)
        event.reactor.connectionToHost(hostname, port, this)
    }

    override fun onConnectionInit(event: Event) {
        val conn = event.connection
        conn.container = "enmasse-bench1"
        val session = conn.session()
        val sender = session.sender("enmasse-bench-sender")

        val target = org.apache.qpid.proton.amqp.messaging.Target()
        target.address = address
        sender.target = target

        conn.open()
        session.open()
        sender.open()
    }

    override fun onDelivery(e: Event) {
        if (e.delivery.remotelySettled()) {
            e.delivery.settle()
        }
    }

    override fun onLinkFlow(e: Event) {
        val snd = e.link as org.apache.qpid.proton.engine.Sender
        //println("Link flow, sending data (credit: ${snd.credit}")
        if (snd.credit > 0) {
            val tag:ByteArray = java.lang.String.valueOf(nextTag++).toByteArray()
            val dlv = snd.delivery(tag)
            snd.send(msgBuffer, 0, msgLen)
            //dlv.settle()
            //println("Ds: ${dlv}")
            snd.advance()
            msgsSent++
        }
    }

    override fun onTransportError(e: Event) {
        println("Transport: ${e.transport.condition.description}")
    }
}
