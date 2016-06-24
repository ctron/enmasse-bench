package enmasse.perf

import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Client(hostname:String, port:Int, address:String, msgSize: Int, val runLength: Long): Runnable
{
    val sendHandler = Sender(hostname, port, address, msgSize)
    val recvHandler = Receiver(hostname, port, address, msgSize)
    var runTime = 0L

    override fun run() {
        val start = System.currentTimeMillis()
        recvHandler.start()
        sendHandler.start()
        Thread.sleep(TimeUnit.SECONDS.toMillis(runLength))
        sendHandler.stop()
        recvHandler.stop()
        val end = System.currentTimeMillis()
        runTime = (end - start) / 1000
    }

    fun result(): Result {
        return Result(recvHandler.msgsReceived, runTime)
    }
}

data class Result(val numMessages: Long, val runTime: Long) {
    fun throughput(): Long {
        return numMessages / runTime
    }
}
