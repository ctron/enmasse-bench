package enmasse.bench.collector

/**
 * @author Ulf Lilleengen
 */
interface AgentMonitor {
    fun listAgents(): List<AgentInfo>
}