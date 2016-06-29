package enmasse.perf

import org.json.JSONObject
import java.util.concurrent.TimeUnit


data class Result(val numMessages: Long, val duration: Long, val totalLatency: Long, val buckets: Array<Bucket>) {
    fun throughput(): Long {
        return numMessages / (duration / 1000)
    }

    fun averageLatency(): Double {
        return totalLatency / numMessages.toDouble()
    }

    fun percentile(p: Double): LongRange {
        var entriesToCount = (numMessages * p).toLong()
        for (bucket in buckets) {
            entriesToCount -= bucket.count()
            if (entriesToCount <= 0) {
                return bucket.range
            }
        }
        throw IllegalStateException("Was never able to count required entries with p = ${p}")
    }

}

interface ResultFormatter {
    fun asString(result: Result): String
}

class DefaultResultFormatter: ResultFormatter {
    override fun asString(result: Result): String {
        val sb = StringBuilder()
        sb.appendln("Result:")
        sb.appendln("Duration:\t\t${result.duration / 1000} s")
        sb.appendln("Messages:\t\t${result.numMessages}")
        sb.appendln("Throughput:\t\t${result.throughput()} msgs/s")
        sb.appendln("Latency avg:\t\t${result.averageLatency()} us")
        sb.appendln("Latency 50p:\t\t${result.percentile(0.5)} us")
        sb.appendln("Latency 75p:\t\t${result.percentile(0.75)} us")
        sb.appendln("Latency 90p:\t\t${result.percentile(0.9)} us")
        sb.appendln("Latency 95p:\t\t${result.percentile(0.95)} us")
        return sb.toString()
     }
}

class JSONResultFormatter: ResultFormatter {
    override fun asString(result: Result): String {
        val json = JSONObject()
        json.put("duration", result.duration / 1000)
        json.put("messages", result.numMessages)
        json.put("throughput", result.throughput())
        json.put("avg", result.averageLatency())
        json.put("50p", result.percentile(0.5))
        json.put("75p", result.percentile(0.75))
        json.put("90p", result.percentile(0.9))
        json.put("95p", result.percentile(0.95))
        return json.toString(0)
    }

}

fun mergeResults(a: Result, b: Result): Result {

    return Result(a.numMessages + b.numMessages, Math.max(a.duration, b.duration), a.totalLatency + b.totalLatency, mergeBuckets(a.buckets, b.buckets))
}

fun mergeBuckets(a: Array<Bucket>, b: Array<Bucket>): Array<Bucket> {
    return if (a.size == 0) {
        b
    } else if (b.size == 0) {
        a
    } else {
        a.forEachIndexed { i, bucket ->
            bucket.increment(b.get(i).count())
        }
        a
    }
}

class MetricRecorder(bucketStep: Long, numBuckets: Long) {
    @Volatile private var totalLatency = 0L
    @Volatile private var numMessages= 0L
    private val buckets: Array<Bucket>
    init {
        buckets = listOf(0.rangeTo(numBuckets - 1).map { i ->
            val start = i * bucketStep
            val end = start + bucketStep
            Bucket(start.rangeTo(end))
        }, listOf(Bucket((bucketStep * numBuckets).rangeTo(Long.MAX_VALUE)))).flatten().toTypedArray()
    }

    fun record(latencyInNanos: Long) {
        val latency = TimeUnit.NANOSECONDS.toMicros(latencyInNanos)
        numMessages++
        totalLatency += latency
        for (bucket in buckets) {
            if (bucket.range.contains(latency)) {
                bucket.increment()
                break
            }
        }
    }

    fun result(duration: Long): Result {
        return Result(numMessages, duration, totalLatency, buckets)
    }
}

class Bucket(val range: LongRange, val initialCount: Long = 0L)
{
    private var count = initialCount
    fun increment(inc: Long = 1L) {
        count += inc
    }

    fun count(): Long {
        return count
    }

    override fun equals(other: Any?): Boolean {
        if (other is Bucket) {
            if (other.range == this.range) {
                return true
            }
        }
        return false
    }
}