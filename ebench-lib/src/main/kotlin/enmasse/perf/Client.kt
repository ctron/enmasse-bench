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

/**
 * @author lulf
 */
class Client(val hostname:String, val port:Int, address:String, msgSize: Int, val duration: Int, val waitTime: Int?, val useMultiplePorts: Boolean): Runnable
{
    val metricRecorder = MetricRecorder(50, 2000)
    val sender = Sender(address, msgSize, metricRecorder, waitTime)
    val recveiver = Receiver(address, msgSize)
    val sendRunner = ClientRunner(hostname, port, sender, duration)
    val recvRunner = ClientRunner(hostname, if (useMultiplePorts) port + 1 else port, recveiver, duration)

    override fun run() {
        recvRunner.start()
        sendRunner.start()
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
}

