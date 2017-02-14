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

package enmasse.bench.collector

import enmasse.perf.*
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * @author Ulf Lilleengen
 */
class Collector(val vertx: Vertx, val monitor: AgentMonitor): TimerTask() {
    val client = vertx.createHttpClient(HttpClientOptions().setConnectTimeout(2000).setIdleTimeout(5000))
    @Volatile var latestSnapshot: Pair<Int, MetricSnapshot>? = null;

    override fun run() {
        snapshot()
    }

    fun snapshot(): java.util.concurrent.Future<Pair<Int, MetricSnapshot>> {
        val promise = CompletableFuture<Pair<Int, MetricSnapshot>>()
        try {
            val agents = monitor.listAgents()
            println("Fetching metrics from : ${agents}")
            CompositeFuture.all(agents.map { agent ->
                val future: Future<MetricSnapshot> = Future.future()
                client.getNow(agent.port, agent.hostname, "/", { response ->
                    // Create an empty buffer
                    val totalBuffer = Buffer.buffer()

                    response.handler({ buffer ->
                        totalBuffer.appendBuffer(buffer);
                    });

                    response.endHandler{ v ->
                        val snapshot = deserializeMetricSnapshot(totalBuffer)
                        future.complete(snapshot)
                    }
                })
                future
            }).setHandler { ar ->
                if (ar.succeeded()) {
                    var merged = emptyMetricSnapshot
                    val snapshots = ar.result().list<MetricSnapshot>()
                    for (snapshot in snapshots) {
                        merged = mergeSnapshots(merged, snapshot)
                    }
                    latestSnapshot = Pair<Int, MetricSnapshot>(snapshots.size, merged)
                    promise.complete(latestSnapshot)
                } else {
                    promise.completeExceptionally(ar.cause())
                    println("Error fetching result from agents: ${ar.cause().message}")
                }
            }
        } catch (e: Exception) {
            println("Error fetching metrics: ${e.message}")
            promise.completeExceptionally(e)
        }
        return promise
    }

    fun latest(): Pair<Int, MetricSnapshot>? {
        return latestSnapshot
    }
}