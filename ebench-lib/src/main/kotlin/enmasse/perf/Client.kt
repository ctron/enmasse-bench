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

import java.time.Year

/**
 * @author lulf
 */
class Client(val hostname:String, val port:Int, address:String, msgSize: Int, val duration: Int, val waitTime: Int, val useMultiplePorts: Boolean, val presettled: Boolean, val splitClients: Boolean): Runnable
{
    val metricRecorder = MetricRecorder(50, 2000)
    val deliveryTracker = DeliveryTracker(metricRecorder, presettled)
    val connectionMonitor = createConnectionMonitor(splitClients)

    val sender = Sender("ebench-send", address, msgSize, waitTime, deliveryTracker, connectionMonitor)
    val recveiver = Receiver("ebench-recv", address, msgSize, deliveryTracker, connectionMonitor)
    @Volatile var sendRunner = ClientRunner(hostname, port, sender, duration)
    @Volatile var recvRunner = ClientRunner(hostname, if (useMultiplePorts) port + 1 else port, recveiver, duration)

    override fun run() {
        while (true) {
            recvRunner.start()
            sendRunner.start()

            println("Entering barrier")
            if (splitClients) {
                if (connectionMonitor.shouldRestart()) {
                    println("Should restart!")
                    Thread.sleep(5000)
                    sendRunner.stop()
                    recvRunner.stop()
                    sendRunner = ClientRunner(hostname, port, sender, duration)
                    recvRunner = ClientRunner(hostname, if (useMultiplePorts) port + 1 else port, recveiver, duration)
                } else {
                    println("Everything is ok, continue!")
                    break;
                }
            }
        }
        metricRecorder.snapshot() // To reset start counter
        sendRunner.stop()
        recvRunner.stop()
    }

    fun snapshot(): MetricSnapshot {
        return if (sendRunner.running()) {
            metricRecorder.snapshot()
        } else {
            metricRecorder.snapshot(sendRunner.endTime())
        }

    }

    private fun  createConnectionMonitor(splitClients: Boolean): ConnectionMonitor {
        if (splitClients) {
            return ClientSplitter("ebench-send", "ebench-recv")
        } else {
            return DummyMonitor()
        }
    }
}

class DummyMonitor : ConnectionMonitor {
    override fun registerConnection(clientId: String, containerId: String): Boolean {
        return true
    }

    override fun shouldRestart(): Boolean {
        return false
    }
}

