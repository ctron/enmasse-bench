package enmasse.perf

/**
 * @author Ulf Lilleengen
 */
interface ResultReporter {
    fun report(result: Result)
}

class StdoutReporter: ResultReporter {
    override fun report(result: Result) {
        val sb = StringBuilder()
        sb.appendln("Result:")
        sb.appendln("Duration:\t\t${result.duration / 1000} s")
        sb.appendln("Messages:\t\t${result.numMessages}")
        sb.appendln("Throughput:\t\t${result.throughput()} msgs/s")
        sb.appendln("Latency avg:\t\t${result.averageLatency()} us")
        sb.appendln("Latency min:\t\t${result.minLatency} us")
        sb.appendln("Latency max:\t\t${result.maxLatency} us")
        sb.appendln("Latency 50p:\t\t${result.percentile(0.5)} us")
        sb.appendln("Latency 75p:\t\t${result.percentile(0.75)} us")
        sb.appendln("Latency 90p:\t\t${result.percentile(0.9)} us")
        sb.appendln("Latency 95p:\t\t${result.percentile(0.95)} us")
        println(sb.toString())
     }
}

class CollectorReporter: ResultReporter {
    override fun report(result: Result) {
        throw UnsupportedOperationException()
    }
}
