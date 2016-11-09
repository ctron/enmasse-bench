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
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker

/**
 * @author lulf
 */
class Sender(val address: String, val msgSize: Int, val waitTime: Int?, val deliveryTracker: DeliveryTracker, val presettled: Boolean): BaseHandler() {
    private var nextTag = 0
    private val msgBuffer: ByteArray = ByteArray(1024)
    private var msgLen = 0
    private var sender:org.apache.qpid.proton.engine.Sender? = null

    init {
        add(Handshaker())
        add(FlowController())

        val msg = Proton.message()
        msg.body = AmqpValue(Binary(1.rangeTo(msgSize).map { a -> a.toByte() }.toByteArray()))
        msgLen = msg.encode(msgBuffer, 0, msgBuffer.size)
    }

    override fun onConnectionInit(event: Event) {
        val conn = event.connection
        conn.container = "ebench-send"
        val session = conn.session()
        sender = session.sender("ebench-send")

        val target = org.apache.qpid.proton.amqp.messaging.Target()
        target.address = address
        sender!!.target = target

        conn.open()
        session.open()
        sender!!.open()

        if (waitTime != null) {
            println("Scheduling wait")
            event.reactor.schedule(waitTime, this)
        }
    }

    override fun onTimerTask(e: Event) {
        if (waitTime != null) {
            sendData(sender!!)
            e.reactor.schedule(waitTime, this)
        }
    }

    override fun onConnectionRemoteOpen(e: Event) {
        println("Sender connected to router ${e.connection.remoteContainer}")
    }

    override fun onDelivery(e: Event) {
        if (e.delivery.remotelySettled()) {
            e.delivery.settle()
            deliveryTracker.onDelivery(e.delivery)
        }
    }

    override fun onLinkFlow(e: Event) {
        val snd = e.link as org.apache.qpid.proton.engine.Sender
        if (waitTime == null && snd.credit > 0) {
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
        if (presettled) {
            dlv.settle()
        }
        //println("Ds: ${dlv}")
        snd.advance()
    }
}

