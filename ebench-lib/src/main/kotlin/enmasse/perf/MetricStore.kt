/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.perf

import java.util.concurrent.TimeUnit

class MetricStore(bucketStep: Long, numBuckets: Long) {
    private var totalLatency = 0L
    private var numMessages= 0L
    private var minLatency = Long.MAX_VALUE
    private var maxLatency = 0L
    private val buckets: List<Bucket>
    init {
        buckets = listOf(0.rangeTo(numBuckets - 1).map { i ->
            val start = i * bucketStep
            val end = start + bucketStep
            Bucket(start.rangeTo(end))
        }, listOf(Bucket((bucketStep * numBuckets).rangeTo(Long.MAX_VALUE)))).flatten()
    }

    fun record(latencyInNanos: Long) {
        val latency = TimeUnit.NANOSECONDS.toMicros(latencyInNanos)
        numMessages++
        totalLatency += latency

        if (latency < minLatency) {
            minLatency = latency
        }

        if (latency > maxLatency) {
            maxLatency = latency
        }

        for (bucket in buckets) {
            if (bucket.range.contains(latency)) {
                bucket.increment(latency)
                break
            }
        }
    }

    fun snapshot(start: Long, end: Long): MetricSnapshot {
        return MetricSnapshot(numMessages, end - start, totalLatency, buckets, minLatency, maxLatency)
    }
}