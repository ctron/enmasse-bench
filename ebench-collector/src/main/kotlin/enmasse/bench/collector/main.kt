package enmasse.bench.collector

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IPod
import com.openshift.restclient.model.IResource
import org.apache.commons.cli.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
fun main(args: Array<String>) {
    val parser = DefaultParser()
    val options = Options()
    options.addOption(createRequiredOption("i", "interval", "Collection interval (in seconds)"))
    options.addOption(createOption("a", "agents", "Comma-separated list of agent <host>:<port>"))

    try {
        val cmd = parser.parse(options, args)
        val interval = java.lang.Long.parseLong(cmd.getOptionValue("i"))
        val intervalMillis = TimeUnit.SECONDS.toMillis(interval)
        val agents = if (cmd.hasOption("a")) parseAgents(cmd.getOptionValue("a")) else discoverAgents()
        println("Found agents: ${agents}")
        val timer = Timer()
        timer.schedule(Collector(agents), intervalMillis, intervalMillis)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench-collector", options)
        System.exit(1)
    }
}

fun discoverAgents(): List<AgentInfo> {
    val openshiftUri = "https://${System.getenv("KUBERNETES_SERVICE_HOST")}:${System.getenv("KUBERNETES_SERVICE_PORT")}"
    val client = ClientBuilder(openshiftUri).authorizationStrategy(TokenAuthorizationStrategy(getAuthenticationToken())).build()
    val pods = client.list<IPod>(ResourceKind.POD, getOpenshiftNamespace(), Collections.singletonMap("name", "ebench-agent"))
    return pods.map { pod -> AgentInfo(pod.ip, pod.containerPorts.iterator().next().containerPort) }
}

private val SERVICEACCOUNT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount"

private fun getOpenshiftNamespace(): String {
    return readFile(File(SERVICEACCOUNT_PATH, "namespace"))
}

private fun getAuthenticationToken():String {
    return readFile(File(SERVICEACCOUNT_PATH, "token"))
}

private fun readFile(file: File): String {
    return String(Files.readAllBytes(file.toPath()))
}

fun parseAgents(agentsString: String): List<AgentInfo> {
    return agentsString.split(",").map { agentString ->
        val components = agentString.split(":")
        AgentInfo(components.get(0), Integer.parseInt(components.get(1)))
    }
}

fun createOption(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
            .hasArg()
            .desc(desc)
            .build()
}

fun createRequiredOption(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
            .hasArg()
            .desc(desc)
            .required()
            .build()
}
