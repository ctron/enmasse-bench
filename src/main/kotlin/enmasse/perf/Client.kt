package enmasse.perf

import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Client(hostname:String, port:Int, address:String, msgSize: Int, val runLength: Int): Runnable
{
    val sendHandler = Sender(hostname, port, address, msgSize, runLength)
    val recvHandler = Receiver(hostname, port, address, msgSize, runLength)

    override fun run() {
        recvHandler.start()
        sendHandler.start()
        sendHandler.stop()
        recvHandler.stop()
    }

    fun result(): Result {
        return Result(recvHandler.msgsReceived, recvHandler.endTime - recvHandler.startTime)
    }
}

data class Result(val numMessages: Long, val runTime: Long) {
    fun throughput(): Long {
        return numMessages / (runTime / 1000)
    }
}
