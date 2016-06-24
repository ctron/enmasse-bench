package enmasse.perf

import java.util.concurrent.TimeUnit

/**
 * @author lulf
 */
class Client(hostname:String, port:Int, address:String, msgSize: Int, val runLength: Long): Runnable
{
    val sendHandler = Sender(hostname, port, address, msgSize)
    val recvHandler = Receiver(hostname, port, address, msgSize)

    override fun run() {
        recvHandler.start()
        sendHandler.start()
        Thread.sleep(TimeUnit.SECONDS.toMillis(runLength))
        sendHandler.stop()
        recvHandler.stop()
        println("Received ${recvHandler.msgsReceived} messages in ${runLength} seconds (${recvHandler.msgsReceived / runLength} msg/s)")
        println("Sent ${sendHandler.msgsSent} messages in ${runLength} seconds (${sendHandler.msgsSent / runLength} msg/s)")
    }
}
