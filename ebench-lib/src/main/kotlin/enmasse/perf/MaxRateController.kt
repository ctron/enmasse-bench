package enmasse.perf

import java.nio.channels.SelectableChannel

class MaxRateController: RateController {
    override fun updateState() {
    }

    override fun channel(): SelectableChannel? {
        return null
    }

    override fun hasSent() {
    }
}