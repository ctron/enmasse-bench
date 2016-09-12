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
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.CoreHandler
import org.apache.qpid.proton.engine.Event
import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
open class ClientRunner(val hostname: String, val port: Int, val clientHandler: CoreHandler, val duration: Int): BaseHandler(), Runnable {
    private val reactor = Proton.reactor(this)
    private val thr = Thread(this)
    private var endTime = 0L
    private @Volatile var running = false

    fun start() {
        running = true
        thr.start()
    }

    override fun onReactorInit(e: Event) {
        e.reactor.connectionToHost(hostname, port, clientHandler)
        e.reactor.schedule(TimeUnit.SECONDS.toMillis(duration.toLong()).toInt(), this)
    }

    override fun onTimerTask(e: Event) {
        running = false
    }

    override fun onReactorFinal(e: Event) {
        endTime = System.currentTimeMillis()
    }

    override fun onTransportError(e: Event) {
        println("Transport error: ${e.transport.condition.description}")
        running = false
    }

    override fun run() {
        reactor.timeout = 3141
        reactor.start()
        while (reactor.process() && running) { }
        reactor.stop()
    }

    fun stop() {
        thr.join()
    }

    fun running(): Boolean {
        return running
    }

    fun endTime(): Long {
        return endTime
    }
}
