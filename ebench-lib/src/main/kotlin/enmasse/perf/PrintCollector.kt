package enmasse.perf

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/**
 * @author Ulf Lilleengen
 */
class PrintCollector(mode: String, val clients: List<Client>, val printInterval: Long?): MetricCollector {
    private val timer = Executors.newScheduledThreadPool(1)
    val printFn: (MetricSnapshot) -> Unit = if (mode.equals("script")) {
        { snapshot: MetricSnapshot -> printSnapshotScript(clients.size, snapshot) }
    } else {
        ::printSnapshot
    }

    override fun start() {
        if (printInterval != null) {
            timer.scheduleAtFixedRate(timerTask {
                printFn.invoke(collectResult(clients))
            }, printInterval, printInterval, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        timer.shutdown()
        printFn.invoke(collectResult(clients))
    }
}

fun printSnapshotScript(clients: Int, metricSnapshot: MetricSnapshot) {
    val sb = StringBuilder()
    sb.append(clients).append(",")
    sb.append(java.lang.String.format("%.2f", metricSnapshot.throughput())).append(",")
    sb.append(metricSnapshot.averageLatency()).append(",")
    sb.append(metricSnapshot.minLatency).append(",")
    sb.append(metricSnapshot.maxLatency).append(",")
    sb.append(metricSnapshot.percentile(0.5)).append(",")
    sb.append(metricSnapshot.percentile(0.95))
    println(sb.toString())
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

