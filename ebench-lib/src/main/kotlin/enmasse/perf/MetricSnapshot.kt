package enmasse.perf

data class MetricSnapshot(val numMessages: Long, val duration: Long, val totalLatency: Long, val buckets: List<Bucket>, val minLatency: Long, val maxLatency: Long) {
    fun throughput(): Long {
        return numMessages / (duration / 1000)
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

