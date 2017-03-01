package enmasse.perf

import java.nio.channels.SelectableChannel

interface RateController {
    fun hasSent()
    fun updateState()
    fun channel(): SelectableChannel?
}