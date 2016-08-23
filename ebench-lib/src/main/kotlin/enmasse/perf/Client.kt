package enmasse.perf

/**
 * @author lulf
 */
class Client(val hostname:String, val port:Int, address:String, msgSize: Int, val duration: Int, val waitTime: Int?, val useMultiplePorts: Boolean): Runnable
{
    val metricRecorder = MetricRecorder(50, 2000)
    val sender = Sender(address, msgSize, metricRecorder, waitTime)
    val recveiver = Receiver(address, msgSize)
    val sendRunner = ClientRunner(hostname, port, sender, duration)
    val recvRunner = ClientRunner(hostname, if (useMultiplePorts) port + 1 else port, recveiver, duration)

    override fun run() {
        recvRunner.start()
        sendRunner.start()
        metricRecorder.snapshot() // To reset start counter
        sendRunner.stop()
        recvRunner.stop()
    }

    fun snapshot(): MetricSnapshot {
        return if (sendRunner.running()) {
            metricRecorder.snapshot()
        } else {
            metricRecorder.snapshot(sendRunner.endTime())
        }

    }
}

