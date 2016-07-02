package enmasse.perf

import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

data class MetricSnapshot(val numMessages: Long, val duration: Long, val totalLatency: Long, val buckets: List<Bucket>, val minLatency: Long, val maxLatency: Long) {
    fun throughput(): Double {
        println("numMessags ${numMessages} duration ${duration}")
        return numMessages / (duration / 1000.toDouble())
    }

    fun averageLatency(): Long {
        return totalLatency / numMessages
    }

    fun percentile(p: Double): Long {
        var entriesToCount = (numMessages * p).toLong()
        for (bucket in buckets) {
            entriesToCount -= bucket.count()
            if (entriesToCount <= 0) {
                return bucket.totalLatency() / bucket.count()
            }
        }
        throw IllegalStateException("Was never able to count required entries with p = ${p}")
    }

}

val emptyMetricSnapshot: MetricSnapshot = MetricSnapshot(0, 0, 0, emptyList(), Long.MAX_VALUE, Long.MIN_VALUE)

fun mergeSnapshots(a: MetricSnapshot, b: MetricSnapshot): MetricSnapshot {
    return MetricSnapshot(a.numMessages + b.numMessages,
            Math.max(a.duration, b.duration),
            a.totalLatency + b.totalLatency,
            mergeBuckets(a.buckets, b.buckets),
            Math.min(a.minLatency, b.minLatency),
            Math.max(a.maxLatency, b.maxLatency))
}

fun mergeBuckets(a: List<Bucket>, b: List<Bucket>): List<Bucket> {
    return if (a.size == 0) {
        b
    } else if (b.size == 0) {
        a
    } else {
        a.mapIndexed { i, bucket ->
            val copy = Bucket(bucket.range)
            copy.increment(bucket.totalLatency() + b.get(i).totalLatency(), bucket.count() + b.get(i).count())
            copy
        }
    }
}

fun serializeMetricSnapshot(buffer: Buffer, snapshot: MetricSnapshot) {
    val root = JsonObject()

    root.put("duration", snapshot.duration)
    root.put("messages", snapshot.numMessages)
    val latencies = JsonObject()
    latencies.put("min", snapshot.minLatency)
    latencies.put("max", snapshot.maxLatency)
    latencies.put("sum", snapshot.totalLatency)
    root.put("latencies", latencies)

    val buckets = JsonArray()
    for (bucket in snapshot.buckets) {
        val jsonBucket = JsonObject()
        jsonBucket.put("totalLatency", bucket.totalLatency())
        jsonBucket.put("count", bucket.count())
        jsonBucket.put("start", bucket.range.start)
        jsonBucket.put("end", bucket.range.endInclusive)
        buckets.add(jsonBucket)
    }
    root.put("buckets", buckets)
    buffer.appendString(root.encode())
}

fun deserializeMetricSnapshot(input: Buffer): MetricSnapshot {
    val root = input.toJsonObject()
    val duration = root.getLong("duration")
    val numMessages = root.getLong("messages")
    val latencies = root.getJsonObject("latencies")
    val sum = latencies.getLong("sum")
    val min = latencies.getLong("min")
    val max = latencies.getLong("max")

    val jsonBuckets = root.getJsonArray("buckets")
    val buckets = jsonBuckets.map { obj ->
        val node = obj as JsonObject
        val total = node.getLong("totalLatency")
        val count = node.getLong("count")
        val start = node.getLong("start")
        val end = node.getLong("end")
        val bucket = Bucket(start.rangeTo(end))
        bucket.increment(total, count)
        bucket
    }
    return MetricSnapshot(numMessages, duration, sum, buckets, min, max)
}
