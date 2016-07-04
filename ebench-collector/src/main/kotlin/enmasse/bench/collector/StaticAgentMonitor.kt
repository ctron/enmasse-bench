package enmasse.bench.collector

class StaticAgentMonitor(val agents: List<AgentInfo>): AgentMonitor {
    override fun listAgents(): List<AgentInfo> {
        return agents
    }
}