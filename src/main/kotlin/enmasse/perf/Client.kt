package enmasse.perf

import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Client(val hostname:String, val port:Int, address:String, msgSize: Int, val duration: Int): Runnable
{
    val sender = Sender(address, msgSize)
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
        return Result(recveiver.msgsReceived, recvRunner.endTime - recvRunner.startTime)
    }
}

data class Result(val numMessages: Long, val duration: Long) {
    fun throughput(): Long {
        return numMessages / (duration / 1000)
    }
}
