package enmasse.perf

import org.apache.qpid.proton.engine.Delivery
import java.util.concurrent.ConcurrentHashMap

class DeliveryTracker(val metricRecorder: MetricRecorder, val presettled: Boolean) {
    val unsetteled: ConcurrentHashMap<ByteArray, Long> = ConcurrentHashMap<ByteArray, Long>()

    fun onDelivery(delivery: Delivery) {
        val endTime = System.nanoTime()
        val startTime = unsetteled.remove(delivery.tag)
        if (startTime != null) {
            metricRecorder.record(endTime - startTime)
        }
    }

    fun onSend(delivery: Delivery, startTime: Long) {
        if (!presettled) {
            unsetteled.put(delivery.tag, startTime);
        }
    }
}