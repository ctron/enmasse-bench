package enmasse.perf

import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FixedRateController(sendRate: Long) : RateController, Runnable {
    private val pipe = Pipe.open()
    private val sink = pipe.sink()
    private val source = pipe.source()
    private val scheduler = Executors.newScheduledThreadPool(1)

    init {
        val sendRateNanos = sendRate.toDouble() / TimeUnit.SECONDS.toNanos(1)
        scheduler.scheduleAtFixedRate(this, 0, (1 / sendRateNanos).toLong(), TimeUnit.NANOSECONDS)
    }

    override fun run() {
        val buffer = ByteBuffer.allocate(8)
        buffer.clear()
        buffer.putLong(System.nanoTime())
        buffer.flip()

        sink.write(buffer)
    }

    override fun channel(): Pipe.SourceChannel? {
        return source
    }

    override fun shutdown() {
        scheduler.shutdown()
    }
}