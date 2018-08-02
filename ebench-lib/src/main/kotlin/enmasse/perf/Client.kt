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

import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.engine.SslDomain
import org.apache.qpid.proton.engine.Transport

/**
 * @author lulf
 */
abstract class Client(val hostname:String, val duration: Int, val connectionMonitor: ConnectionMonitor): Runnable, BaseHandler()
{
    val metricRecorder = MetricRecorder()
    @Volatile var runner = ClientRunner(getHost(hostname), getPort(hostname), this, duration)

    private fun getHost(hostname: String): String {
        val parts = hostname.split(":")
        return parts[0];
    }

    private fun getPort(hostname: String): Int {
        val parts = hostname.split(":")
        if (parts != null && parts.size > 1) {
            return Integer.parseInt(parts[1]);
        } else {
            return 5672;
        }
    }

    override fun run() {
        runner.start()

        waitForSeparateConnections()

        metricRecorder.snapshot() // To reset start counter

        runner.stop(false)
    }

    abstract fun stop()

    private fun waitForSeparateConnections() {
        while (true) {
            if (connectionMonitor.shouldRestart()) {
                runner.stop(true)
                runner = ClientRunner(getHost(hostname), getPort(hostname), this, duration)
                runner.start()
            } else {
                break;
            }
        }
    }

    fun snapshot(): MetricSnapshot {
        return if (runner.running()) {
            metricRecorder.snapshot()
        } else {
            metricRecorder.snapshot(runner.endTime())
        }

    }

    override fun onConnectionBound(e: Event) {

        val unsecure = java.lang.Boolean.parseBoolean (System.getenv("ENABLE_UNSECURE_TLS"))
        if (unsecure) {

            println("Enabling unsecure TLS")

            val transport = e.transport

            val domain = SslDomain.Factory.create()
            domain.init(SslDomain.Mode.CLIENT)

            domain.peerAuthentication = SslDomain.VerifyMode.ANONYMOUS_PEER
            transport.ssl(domain)
        }

        super.onConnectionBound(e)
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

