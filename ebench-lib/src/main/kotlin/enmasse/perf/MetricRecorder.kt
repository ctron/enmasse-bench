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

import org.HdrHistogram.*

class MetricRecorder(val numberOfSignificantValueDigits: Int = 5) {
    private @Volatile var histogram: AbstractHistogram

    init {
        histogram = ConcurrentHistogram(numberOfSignificantValueDigits)
        histogram.startTimeStamp = System.currentTimeMillis()
    }

    fun snapshot(stop:Long = System.currentTimeMillis()): MetricSnapshot {
        histogram.endTimeStamp = stop
        val snapshot = MetricSnapshot(histogram)
        histogram = ConcurrentHistogram(numberOfSignificantValueDigits)
        histogram.startTimeStamp = System.currentTimeMillis()
        return snapshot
    }

    fun record(latency: Long) {
        histogram.recordValue(latency)
    }
}