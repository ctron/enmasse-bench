package enmasse.perf

import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Client(hostname:String, port:Int, address:String, msgSize: Int, val runLength: Long)
{
    val sendHandler = Sender(hostname, port, address, msgSize)
    val recvHandler = Receiver(hostname, port, address, msgSize)

    fun run() {
        recvHandler.start()
        sendHandler.start()
        Thread.sleep(TimeUnit.SECONDS.toMillis(runLength))
        sendHandler.stop()
        recvHandler.stop()
        println("Received ${recvHandler.msgsReceived} messages in ${runLength} seconds (${recvHandler.msgsReceived / runLength} msg/s)")
    }
}
