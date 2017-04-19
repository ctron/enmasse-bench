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

import io.vertx.core.buffer.Buffer
import org.HdrHistogram.AbstractHistogram
import org.HdrHistogram.Histogram
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.Deflater
import java.util.zip.Inflater

data class MetricSnapshot(val histogram: AbstractHistogram) {
    fun throughput(): Double {
        return histogram.totalCount / ((histogram.endTimeStamp - histogram.startTimeStamp) / 1000.toDouble())
    }

    private fun toMillis(value: Double): Double {
        return value / TimeUnit.MILLISECONDS.toNanos(1)
    }

    fun averageLatency(): Double {
        return toMillis(histogram.mean)
    }

    fun minLatency(): Double {
        return toMillis(histogram.minValue.toDouble())
    }

    fun maxLatency(): Double {
        return toMillis(histogram.maxValueAsDouble)
    }

    fun duration(): Long {
        return histogram.endTimeStamp - histogram.startTimeStamp
    }

    fun numMessages(): Long {
        return histogram.totalCount
    }

    fun percentile(p: Double): Double {
        return toMillis(histogram.getValueAtPercentile(p).toDouble())
    }
}

val emptyMetricSnapshot: MetricSnapshot = MetricSnapshot(Histogram(5))

fun mergeSnapshots(a: MetricSnapshot, b: MetricSnapshot): MetricSnapshot {
    val histCopy = a.histogram.copy()
    histCopy.add(b.histogram)
    return MetricSnapshot(histCopy)
}

fun serializeMetricSnapshot(buffer: Buffer, snapshot: MetricSnapshot) {
    val baos = ByteArrayOutputStream()
    val out = ObjectOutputStream(baos)
    out.writeObject(snapshot.histogram)
    val compresser = Deflater()
    compresser.setInput(baos.toByteArray())
    compresser.finish()

    val output = ByteArray(1024 * 1024)
    val compressedLen = compresser.deflate(output)
    buffer.appendBytes(output, 0, compressedLen)
}

fun deserializeMetricSnapshot(input: Buffer): MetricSnapshot {
    val decompresser = Inflater()
    decompresser.setInput(input.bytes)
    decompresser.finished()

    val output = ByteArray(1024 * 1024)
    val decompressedLen = decompresser.inflate(output)

    val bis = ByteArrayInputStream(output, 0, decompressedLen)
    val input = ObjectInputStream(bis)
    val histogram = input.readObject() as AbstractHistogram
    return MetricSnapshot(histogram)
}

fun printSnapshotScriptable(clients: Int, metricSnapshot: MetricSnapshot) {
    val sb = StringBuilder()
    sb.append(clients).append(",")
    sb.append(java.lang.String.format("%.2f", metricSnapshot.throughput())).append(",")
    sb.append(metricSnapshot.averageLatency()).append(",")
    sb.append(metricSnapshot.minLatency()).append(",")
    sb.append(metricSnapshot.maxLatency()).append(",")
    sb.append(metricSnapshot.percentile(50.0)).append(",")
    sb.append(metricSnapshot.percentile(99.0))
    sb.append(metricSnapshot.percentile(99.9))
    sb.append(metricSnapshot.percentile(99.99))
    sb.append(metricSnapshot.percentile(99.999))
    sb.append(metricSnapshot.percentile(99.9999))
    sb.append(metricSnapshot.percentile(99.99999))
    println(sb.toString())
}

fun printSnapshotPretty(clients: Int, metricSnapshot: MetricSnapshot) {
    val sb = StringBuilder()
    sb.appendln("Result:")
    sb.appendln("Clients:\t\t${clients}")
    sb.appendln("Duration:\t\t${java.lang.String.format("%.2f", metricSnapshot.duration() / 1000.toDouble())} s")
    sb.appendln("Messages:\t\t${metricSnapshot.numMessages()}")
    sb.appendln("Throughput:\t\t${java.lang.String.format("%.2f", metricSnapshot.throughput())} msgs/s")
    sb.appendln("Latency avg:\t\t${metricSnapshot.averageLatency()} ms")
    sb.appendln("Latency min:\t\t${metricSnapshot.minLatency()} ms")
    sb.appendln("Latency max:\t\t${metricSnapshot.maxLatency()} ms")
    sb.appendln("Latency 50p:\t\t${metricSnapshot.percentile(5.0)} ms")
    sb.appendln("Latency 90p:\t\t${metricSnapshot.percentile(90.0)} ms")
    sb.appendln("Latency 95p:\t\t${metricSnapshot.percentile(95.0)} ms")
    sb.appendln("Latency 99p:\t\t${metricSnapshot.percentile(99.0)} ms")
    sb.appendln("Latency 99.9p:\t\t${metricSnapshot.percentile(99.9)} ms")
    sb.appendln("Latency 99.99p:\t\t${metricSnapshot.percentile(99.99)} ms")
    sb.appendln("Latency 99.999p:\t${metricSnapshot.percentile(99.999)} ms")
    sb.appendln("Latency 99.9999p:\t${metricSnapshot.percentile(99.9999)} ms")
    sb.appendln("Latency 99.99999p:\t${metricSnapshot.percentile(99.99999)} ms")
    println(sb.toString())
}

