package enmasse.perf

/**
 * @author Ulf Lilleengen
 */
interface MetricCollector {

    fun start()
    fun stop()

    fun collectResult(clients: List<Client>): MetricSnapshot {
        return clients.map(Client::snapshot)
                .foldRight(emptyMetricSnapshot, { a, b -> mergeSnapshots(a, b)})
    }
}

