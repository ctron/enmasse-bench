/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.perf

import org.apache.qpid.proton.Proton
import org.apache.qpid.proton.amqp.Binary
import org.apache.qpid.proton.amqp.Symbol
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.engine.Sender
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.nio.ByteBuffer
import java.nio.channels.Pipe

/**
 * @author lulf
 */
class Sender(val clientId: String,
             hostname: String,
             val address: String,
             val isTopic: Boolean,
             msgSize: Int,
             duration: Int,
             presettled: Boolean,
             val rateController: RateController,
             connectionMonitor: ConnectionMonitor):
        Client(hostname, duration, connectionMonitor) {

    val deliveryTracker = DeliveryTracker(metricRecorder, presettled)
    private val buffer = ByteBuffer.allocate(8)
    private var nextTag = 0
    private val msgBuffer: ByteArray = ByteArray(msgSize + 1024)
    private var msgLen = 0
    private var localCredits = 0L;
    private var sender:org.apache.qpid.proton.engine.Sender? = null
    @Volatile private var aborted = true

    init {
        add(Handshaker())
        add(FlowController())

        val msg = Proton.message()
        msg.body = AmqpValue(Binary(1.rangeTo(msgSize).map { a -> a.toByte() }.toByteArray()))
        msgLen = msg.encode(msgBuffer, 0, msgBuffer.size)
    }

    override fun onConnectionInit(event: Event) {
        aborted = false
        val conn = event.connection
        conn.container = clientId
        conn.open()
    }

    fun aborted(): Boolean {
        return aborted
    }

    override fun onConnectionRemoteOpen(e: Event) {
        println("Sender connected to router ${e.connection.remoteContainer}")
        if (!connectionMonitor.registerConnection(clientId, e.connection.remoteContainer)) {
            println("Aborting sender connection")
            e.connection.close()
            aborted = true
        }
        val session = e.connection.session()
        sender = session.sender(clientId)

        val target = org.apache.qpid.proton.amqp.messaging.Target()
        target.address = address
        if (isTopic) {
            target.setCapabilities(Symbol.getSymbol("topic"))
        }
        sender!!.target = target

        session.open()
        sender!!.open()

        if (rateController.channel() != null) {
            val selectable = e.reactor.selectable()
            setHandler(selectable, this)
            e.reactor.update(selectable)
        }
    }

    override fun onDelivery(e: Event) {
        if (e.delivery.remotelySettled()) {
            e.delivery.settle()
            deliveryTracker.onDelivery(e.delivery)
        }
    }

    override fun onSelectableInit(e: Event) {
        val selectable = e.selectable
        selectable.channel = rateController.channel()
        selectable.isReading = true
        e.reactor.update(selectable)
    }

    override fun onSelectableReadable(e: Event) {
        val selectable = e.selectable
        val channel:Pipe.SourceChannel = selectable.channel as Pipe.SourceChannel

        val amount = channel.read(buffer)
        buffer.flip()
        localCredits = buffer.long
        buffer.clear()
        sendData(sender!!)
    }

    override fun onLinkFlow(e: Event) {
        val snd = e.link as org.apache.qpid.proton.engine.Sender
        sendData(snd)
    }

    override fun onTransportError(e: Event) {
        println("Transport error: ${e.transport.condition.description}")
    }

    fun sendData(snd: Sender) {
        if (snd.credit > 0 && (rateController.channel() == null || localCredits > 0)) {
            val tag: ByteArray = java.lang.String.valueOf(nextTag++).toByteArray()
            val dlv = snd.delivery(tag)
            deliveryTracker.onSend(dlv)
            snd.send(msgBuffer, 0, msgLen)
            deliveryTracker.onSent(dlv)
            snd.advance()
            rateController.hasSent()
            localCredits--
        }
    }
}

