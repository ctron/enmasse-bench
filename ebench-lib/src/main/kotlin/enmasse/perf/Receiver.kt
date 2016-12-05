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

import org.apache.qpid.proton.amqp.UnsignedInteger
import org.apache.qpid.proton.amqp.messaging.Accepted
import org.apache.qpid.proton.amqp.messaging.TerminusDurability
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.engine.Receiver
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * @author lulf
 */
class Receiver(val clientId: String, val address: String, msgSize: Int, val deliveryTracker: DeliveryTracker, val connectionMonitor: ConnectionMonitor): BaseHandler() {

    val buffer = ByteArray(msgSize)

    init {
        add(Handshaker())
        add(FlowController())
    }

    override fun onConnectionInit(event: Event) {
        val conn = event.connection
        conn.container = clientId

        conn.open()
    }

    override fun onConnectionRemoteOpen(e: Event) {
        println("Receiver connected to router ${e.connection.remoteContainer}")
        if (!connectionMonitor.registerConnection(clientId, e.connection.remoteContainer)) {
            println("Aborting receiver connection")
            e.connection.close()
        }
        val conn = e.connection
        val session = conn.session()
        val recv = session.receiver(clientId)

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

        session.open()
        recv.open()
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
            deliveryTracker.onReceiverDelivery(delivery)
        }
    }
}