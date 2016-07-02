package enmasse.perf

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/**
 * @author Ulf Lilleengen
 */
class PrintCollector(val clients: List<Client>, val printInterval: Long?): MetricCollector {
    private val timer = Timer()

    override fun start() {
        if (printInterval != null) {
            val printIntervalMillis = TimeUnit.SECONDS.toMillis(printInterval)
            timer.schedule(timerTask {
                val metricSnapshot = collectResult(clients)
                printSnapshot(metricSnapshot)
            }, printIntervalMillis, printIntervalMillis)
        }
    }

    override fun stop() {
        timer.cancel()
    }


}

fun printSnapshot(metricSnapshot: MetricSnapshot) {
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

