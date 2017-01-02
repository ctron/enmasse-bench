package enmasse.perf

import java.util.concurrent.CyclicBarrier

/**
 * Decides if clients should be split
 */
class ClientSplitter (clientIds: List<String>): ConnectionMonitor {

    @Volatile var isEqual: Boolean = false
    val barrier: CyclicBarrier = CyclicBarrier(clientIds.size + 1, Runnable {
        isEqual = java.util.HashSet<String>(idMap.values).size == 1
        println("Barrier completed, id map: ${idMap}")
    })

    val idMap = java.util.HashMap<String, String>()

    init {
        for (clientId in clientIds) {
            idMap.put(clientId, clientId)
        }
    }

    override fun registerConnection(clientId: String, containerId: String): Boolean {
        idMap.put(clientId, containerId)
        barrier.await()
        return !isEqual
    }

    override fun shouldRestart(): Boolean {
        barrier.await()
        return isEqual
    }
}