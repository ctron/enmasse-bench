package enmasse.perf

class Bucket(val range: LongRange)
{
    private var count = 0L
    private var totalLatency = 0L

    fun increment(latency: Long, inc: Long = 1L) {
        count += inc
        totalLatency += latency
    }

    fun count(): Long {
        return count
    }

    fun totalLatency(): Long {
        return totalLatency
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