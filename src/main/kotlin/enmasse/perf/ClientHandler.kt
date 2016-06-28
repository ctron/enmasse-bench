package enmasse.perf

import org.apache.qpid.proton.engine.CoreHandler

interface ClientHandler : CoreHandler {
    fun messageCount(): Long
}