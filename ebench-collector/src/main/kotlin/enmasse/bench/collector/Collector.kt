package enmasse.bench.collector

import enmasse.perf.*
import io.vertx.core.CompositeFuture
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import java.util.*

/**
 * @author Ulf Lilleengen
 */
class Collector(val monitor: AgentMonitor): TimerTask() {
    val vertx = Vertx.vertx()
    val client = vertx.createHttpClient(HttpClientOptions().setConnectTimeout(2000).setIdleTimeout(5000))

    override fun run() {
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
                    printSnapshotPretty(snapshots.size, merged)
                } else {
                    println("Error fetching result from agents: ${ar.cause().message}")
                }
            }
        } catch (e: Exception) {
            println("Error fetching metrics: ${e.message}")
        }
    }
}