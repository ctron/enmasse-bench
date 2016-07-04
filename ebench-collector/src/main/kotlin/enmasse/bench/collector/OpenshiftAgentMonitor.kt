package enmasse.bench.collector

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.IClient
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IPod
import java.io.File
import java.nio.file.Files
import java.util.*

class OpenshiftAgentMonitor: AgentMonitor {
    val client: IClient
    init {
        val openshiftUri = "https://${System.getenv("KUBERNETES_SERVICE_HOST")}:${System.getenv("KUBERNETES_SERVICE_PORT")}"
        client = ClientBuilder(openshiftUri).authorizationStrategy(TokenAuthorizationStrategy(getAuthenticationToken())).build()
    }

    override fun listAgents(): List<AgentInfo> {
        val pods = client.list<IPod>(ResourceKind.POD, getOpenshiftNamespace(), Collections.singletonMap("name", "ebench-agent"))
        return pods.map { pod -> AgentInfo(pod.ip, pod.containerPorts.iterator().next().containerPort) }
    }

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
