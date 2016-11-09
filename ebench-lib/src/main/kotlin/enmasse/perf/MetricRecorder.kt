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

    fun record() {
        currentStore.record()
    }
}