package enmasse.perf

import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.nio.channels.SelectableChannel

class PidRateController(val pFactor: Double, val iFactor: Double, val sendRate: Long): RateController {
    @Volatile private var numSent = 0L
    private var sendGoal = sendRate
    private val pipe = Pipe.open()
    private val sink = pipe.sink()
    private val source = pipe.source()
    private val buffer = ByteBuffer.allocate(8)
    private var totalError = 0L

    init {
        println("Configuring with send rate ${sendRate}/s")
    }

    override fun hasSent() {
        numSent++
    }

    override fun updateState() {
        val pOut = pFactor * (sendRate - numSent)
        totalError += (sendRate - numSent)
        val iOut = iFactor * totalError

        sendGoal += (pOut.toLong() + iOut.toLong())


        notifyPipe()
//        println("${numSent} messages since last call. pOut: ${pOut}, iOut: ${iOut} send goal is now ${sendGoal}")


        numSent = 0
    }

    private fun notifyPipe() {
        buffer.clear()
        buffer.putLong(sendGoal)
        buffer.flip()

        sink.write(buffer)
    }

    override fun channel(): SelectableChannel {
        return source
    }
}