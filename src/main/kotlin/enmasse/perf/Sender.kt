package enmasse.perf

import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Sender(val hostname:String, val port: Int, val address: String, val msgSize: Int): ClientHandler() {

    var nextTag = 0
    val msgData: ByteArray = 1.rangeTo(msgSize).map { a -> a.toByte() }.toByteArray()
    var msgsSent = 0

    override fun onReactorInit(event: Event) {
        event.reactor.connectionToHost(hostname, port, this)
    }

    override fun onConnectionInit(event: Event) {
        println("Connection init sender")
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

    override fun onLinkFlow(e: Event) {
        val snd = e.link as org.apache.qpid.proton.engine.Sender
        //println("Link flow, sending data (credit: ${snd.credit}")
        if (snd.credit > 0) {
            val tag:ByteArray = java.lang.String.valueOf(nextTag++).toByteArray()
            val dlv = snd.delivery(tag)
            snd.send(msgData, 0, msgSize)
            snd.advance()
            msgsSent++
        }
    }

    override fun onTransportError(e: Event) {
        println("Error during transport: ${e.transport.condition.description}")
    }
}

