package enmasse.perf

/**
 * @author lulf
 */
class Client(val hostname:String, val port:Int, address:String, msgSize: Int, val duration: Int): Runnable
{
    val metricRecorder = MetricRecorder(10, 100000)
    val sender = Sender(address, msgSize, metricRecorder)
    val recveiver = Receiver(address, msgSize)
    val sendRunner = ClientRunner(hostname, port, sender, duration)
    val recvRunner = ClientRunner(hostname, port, recveiver, duration)

    override fun run() {
        recvRunner.start()
        sendRunner.start()
        sendRunner.stop()
        recvRunner.stop()
    }

    fun result(): Result {
        return metricRecorder.result(sendRunner.currentRuntime())
    }
}

