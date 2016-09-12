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

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/**
 * @author Ulf Lilleengen
 */
class TimedCollector(val clients: List<Client>, val timerInterval: Long?, val snapshotHandler: (Int, MetricSnapshot) -> Unit): MetricCollector {
    private val timer = Executors.newScheduledThreadPool(1)

    override fun start() {
        if (timerInterval != null) {
            timer.scheduleAtFixedRate(timerTask {
                snapshotHandler.invoke(clients.size, collectResult(clients))
            }, timerInterval, timerInterval, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        timer.shutdown()
        snapshotHandler.invoke(clients.size, collectResult(clients))
    }
}


