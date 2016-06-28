package enmasse.perf

data class Result(val numMessages: Long, val duration: Long) {
    fun throughput(): Long {
        return numMessages / (duration / 1000)
    }
}