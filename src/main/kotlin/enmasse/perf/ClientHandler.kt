package enmasse.perf

import org.apache.qpid.proton.Proton
import org.apache.qpid.proton.engine.BaseHandler
import org.apache.qpid.proton.engine.Event
import org.apache.qpid.proton.reactor.FlowController
import org.apache.qpid.proton.reactor.Handshaker

/**
 * @author lulf
 */
open class ClientHandler : BaseHandler(), Runnable {
    val reactor = Proton.reactor(this)
    val thr = Thread(this)

    init {
        add(Handshaker())
        add(FlowController())
    }

    fun start() {
        thr.start()
    }

    override fun run() {
        try {
            reactor.run()
        } catch (e: Exception) {
            println("Stopping client: ${e.message}")
        }
    }

    fun stop() {
        thr.interrupt()
        thr.join()
    }
}

class LoggingHandler(val id:String): BaseHandler() {
    override fun onUnhandled(event: Event) {
        println("${id}: ${event}")
    }
}