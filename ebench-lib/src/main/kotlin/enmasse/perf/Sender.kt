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
import org.apache.qpid.proton.amqp.messaging.AmqpValue
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker

/**
 * @author lulf
 */
class Sender(val clientId: String,
             hostname: String,
             val address: String,
             msgSize: Int,
             duration: Int,
             val waitTime: Int,
             presettled: Boolean,
             connectionMonitor: ConnectionMonitor):
        Client(hostname, duration, connectionMonitor) {

    val deliveryTracker = DeliveryTracker(metricRecorder, presettled)
    private var nextTag = 0
    private val msgBuffer: ByteArray = ByteArray(1024)
    private var msgLen = 0
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

    override fun onTimerTask(e: Event) {
        if (waitTime != null) {
            sendData(sender!!)
            e.reactor.schedule(waitTime, this)
        }
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
        sender!!.target = target

        session.open()
        sender!!.open()

        if (waitTime > 0) {
            println("Scheduling wait")
            e.reactor.schedule(waitTime, this)
        }
    }

    override fun onDelivery(e: Event) {
        if (e.delivery.remotelySettled()) {
            e.delivery.settle()
            deliveryTracker.onDelivery(e.delivery)
        }
    }

    override fun onLinkFlow(e: Event) {
        val snd = e.link as org.apache.qpid.proton.engine.Sender
        if (waitTime == 0 && snd.credit > 0) {
            sendData(snd)
        }
    }

    override fun onTransportError(e: Event) {
        println("Transport error: ${e.transport.condition.description}")
    }

    fun sendData(snd: org.apache.qpid.proton.engine.Sender) {
        val tag:ByteArray = java.lang.String.valueOf(nextTag++).toByteArray()
        val dlv = snd.delivery(tag)
        deliveryTracker.onSend(dlv)
        snd.send(msgBuffer, 0, msgLen)
        deliveryTracker.onSent(dlv)
        snd.advance()
    }
}

