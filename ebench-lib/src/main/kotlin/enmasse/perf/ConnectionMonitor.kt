package enmasse.perf

interface ConnectionMonitor {
    /**
     * Registers a client connection
     *
     * @return false if already registered, true if OK to proceed.
     */
    fun registerConnection(clientId: String, containerId: String): Boolean

    /**
     * Check if the connections should be restarted
     */
    fun shouldRestart(): Boolean
}