package enmasse.perf

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

/**
 * @author Ulf Lilleengen
 */
class TimedCollector(val clients: List<Client>, val timerInterval: Long?, val snapshotHandler: (Int, MetricSnapshot) -> Unit): MetricCollector {
    private val timer = Executors.newScheduledThreadPool(1)

    override fun start() {
        if (timerInterval != null) {
            timer.scheduleAtFixedRate(timerTask {
                snapshotHandler.invoke(clients.size, collectResult(clients))
            }, timerInterval, timerInterval, TimeUnit.SECONDS)
        }
    }

    override fun stop() {
        timer.shutdown()
        snapshotHandler.invoke(clients.size, collectResult(clients))
    }
}


