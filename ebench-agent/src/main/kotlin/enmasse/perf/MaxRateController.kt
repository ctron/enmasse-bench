package enmasse.perf

import java.nio.channels.Pipe

class MaxRateController : RateController {
    override fun start() {
    }

    override fun shutdown() {
    }

    override fun channel(): Pipe.SourceChannel? {
        return null
    }
}