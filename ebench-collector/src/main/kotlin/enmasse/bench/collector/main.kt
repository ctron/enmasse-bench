package enmasse.bench.collector

import org.apache.commons.cli.*
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
fun main(args: Array<String>) {
    val parser = DefaultParser()
    val options = Options()
    options.addOption(createRequiredOption("i", "interval", "Collection interval (in seconds)"))
    options.addOption(createRequiredOption("a", "agents", "Comma-separated list of agent <host>:<port>"))

    try {
        val cmd = parser.parse(options, args)
        val interval = java.lang.Long.parseLong(cmd.getOptionValue("i"))
        val intervalMillis = TimeUnit.SECONDS.toMillis(interval)
        val agents = parseAgents(cmd.getOptionValue("a"))
        val timer = Timer()
        timer.schedule(Collector(agents), intervalMillis, intervalMillis)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench-collector", options)
        System.exit(1)
    }
}

fun parseAgents(agentsString: String): List<AgentInfo> {
    return agentsString.split(",").map { agentString ->
        val components = agentString.split(":")
        AgentInfo(components.get(0), Integer.parseInt(components.get(1)))
    }
}

fun createRequiredOption(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
            .hasArg()
            .desc(desc)
            .required()
            .build()
}
