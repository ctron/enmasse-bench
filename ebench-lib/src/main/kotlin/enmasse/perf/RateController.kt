package enmasse.perf

import java.nio.channels.Pipe

interface RateController {
    fun channel(): Pipe.SourceChannel?
    fun shutdown()
}