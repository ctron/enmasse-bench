package enmasse.perf

import java.nio.channels.Pipe

class MaxRateController : RateController {
    override fun shutdown() {
    }

    override fun channel(): Pipe.SourceChannel? {
        return null
    }
}