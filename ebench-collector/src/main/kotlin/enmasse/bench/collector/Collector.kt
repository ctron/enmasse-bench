package enmasse.bench.collector

import enmasse.perf.MetricSnapshot
import enmasse.perf.deserializeMetricSnapshot
import enmasse.perf.mergeSnapshots
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
class Collector(val monitor: AgentMonitor): TimerTask() {
    val vertx = Vertx.vertx()
    val client = vertx.createHttpClient()

    override fun run() {
        val agents = monitor.listAgents()
        val queue = java.util.concurrent.ArrayBlockingQueue<MetricSnapshot>(agents.size)
        agents.forEach { agent ->
            client.getNow(agent.port, agent.hostname, "/", { response ->
                // Create an empty buffer
                val totalBuffer = Buffer.buffer()

                response.handler({ buffer ->
                    totalBuffer.appendBuffer(buffer);
                });

                response.endHandler({ v ->
                    println("Got snapshot from remote")
                    val snapshot = deserializeMetricSnapshot(totalBuffer)
                    queue.put(snapshot)
                })
            })
        }

        var merged:MetricSnapshot? = null
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
        val sb = StringBuilder()
        sb.appendln("Result:")
        sb.appendln("Duration:\t\t${java.lang.String.format("%.2f", metricSnapshot.duration / 1000.toDouble())} s")
        sb.appendln("Messages:\t\t${metricSnapshot.numMessages}")
        sb.appendln("Throughput:\t\t${java.lang.String.format("%.2f", metricSnapshot.throughput())} msgs/s")
        sb.appendln("Latency avg:\t\t${metricSnapshot.averageLatency()} us")
        sb.appendln("Latency min:\t\t${metricSnapshot.minLatency} us")
        sb.appendln("Latency max:\t\t${metricSnapshot.maxLatency} us")
        sb.appendln("Latency 50p:\t\t${metricSnapshot.percentile(0.5)} us")
        sb.appendln("Latency 75p:\t\t${metricSnapshot.percentile(0.75)} us")
        sb.appendln("Latency 90p:\t\t${metricSnapshot.percentile(0.9)} us")
        sb.appendln("Latency 95p:\t\t${metricSnapshot.percentile(0.95)} us")
        println(sb.toString())
    }
}