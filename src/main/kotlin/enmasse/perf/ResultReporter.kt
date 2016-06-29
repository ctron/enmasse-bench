package enmasse.perf

/**
 * @author Ulf Lilleengen
 */
interface ResultReporter {
    fun report(metricSnapshot: MetricSnapshot)
}

class StdoutReporter: ResultReporter {
    override fun report(metricSnapshot: MetricSnapshot) {
        val sb = StringBuilder()
        sb.appendln("Result:")
        sb.appendln("Duration:\t\t${java.lang.String.format("%.2f", metricSnapshot.duration / 1000.toDouble())} s")
        sb.appendln("Messages:\t\t${metricSnapshot.numMessages}")
        sb.appendln("Throughput:\t\t${metricSnapshot.throughput()} msgs/s")
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

class CollectorReporter: ResultReporter {
    override fun report(metricSnapshot: MetricSnapshot) {
        throw UnsupportedOperationException()
    }
}
