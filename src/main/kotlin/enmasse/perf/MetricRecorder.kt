package enmasse.perf

class MetricRecorder(val bucketStep: Long, val numBuckets: Long) {
    @Volatile private var currentStore = MetricStore(bucketStep, numBuckets)
    @Volatile private var resetTime = 0L

    fun snapshot(stop:Long = System.currentTimeMillis()): MetricSnapshot {
        val store = currentStore
        val start = resetTime
        currentStore = MetricStore(bucketStep, numBuckets)
        resetTime = System.currentTimeMillis()
        return store.snapshot(start, stop)
    }

    fun record(latency: Long) {
        currentStore.record(latency)
    }
}