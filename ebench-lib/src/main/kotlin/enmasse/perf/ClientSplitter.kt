package enmasse.perf

import java.util.concurrent.CyclicBarrier

/**
 * Decides if clients should be split
 */
class ClientSplitter (val senderId: String, val receiverId: String): ConnectionMonitor {

    @Volatile var isEqual: Boolean = false
    val barrier: CyclicBarrier = CyclicBarrier(3, Runnable {
        isEqual = idMap[senderId].equals(idMap[receiverId])
        println("Barrier completed, sender id ${idMap[senderId]}, receiver id ${idMap[receiverId]}")
    })

    val idMap = java.util.HashMap<String, String>()

    init {
        idMap.put(senderId, senderId)
        idMap.put(receiverId, receiverId)
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