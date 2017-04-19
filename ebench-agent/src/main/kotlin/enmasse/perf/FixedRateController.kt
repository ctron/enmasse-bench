package enmasse.perf

import java.nio.ByteBuffer
import java.nio.channels.Pipe
import java.util.concurrent.TimeUnit

class FixedRateController(val sendRate: Long) : RateController, Runnable {
    private val pipe = Pipe.open()
    private val sink = pipe.sink()
    private val source = pipe.source()
    private @Volatile var running = true
    private val thread = Thread(this)

    override fun start() {
        thread.start()
    }

    override fun run() {
        val sendRateNanos = sendRate.toDouble() / TimeUnit.SECONDS.toNanos(1)
        val waitTimeNanos = (1.0 / sendRateNanos).toLong()
        println("Waiting ${waitTimeNanos} ns between requests")
        var startTime = System.nanoTime()
        while (running) {
            startTime += waitTimeNanos
            spinWaitUntil(startTime)
            notify(startTime)
        }
    }

    private fun spinWaitUntil(deadline: Long) {
        while (deadline > System.nanoTime()) {
        }
    }

    private fun notify(startTime: Long) {
        val buffer = ByteBuffer.allocate(8)
        buffer.clear()
        buffer.putLong(startTime)
        buffer.flip()

        sink.write(buffer)
    }

    override fun channel(): Pipe.SourceChannel? {
        return source
    }

    override fun shutdown() {
        running = false;
        thread.join()
    }
}