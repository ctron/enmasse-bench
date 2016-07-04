package enmasse.bench.collector

import enmasse.perf.MetricSnapshot
import enmasse.perf.deserializeMetricSnapshot
import enmasse.perf.mergeSnapshots
import enmasse.perf.printSnapshot
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import java.util.*
import java.util.concurrent.TimeUnit

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
            val queue = java.util.concurrent.ArrayBlockingQueue<MetricSnapshot>(agents.size)
            agents.forEach { agent ->
                client.getNow(agent.port, agent.hostname, "/", { response ->
                    // Create an empty buffer
                    val totalBuffer = Buffer.buffer()

                    response.handler({ buffer ->
                        totalBuffer.appendBuffer(buffer);
                    });

                    response.endHandler({ v ->
                        val snapshot = deserializeMetricSnapshot(totalBuffer)
                        queue.put(snapshot)
                    })
                })
            }

            var merged: MetricSnapshot? = null
            var numMerged = 0
            while (numMerged < agents.size) {
                val snapshot = queue.poll(60, TimeUnit.SECONDS)
                if (merged == null) {
                    merged = snapshot
                } else {
                    merged = mergeSnapshots(merged, snapshot)
                }
                numMerged++
            }

            val metricSnapshot = merged!!
            printSnapshot(metricSnapshot)
        } catch (e: Exception) {
            println("Error fetching metrics: ${e.message}")
        }
    }
}