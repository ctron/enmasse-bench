/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package enmasse.bench.collector

import com.openshift.restclient.ClientBuilder
import com.openshift.restclient.IClient
import com.openshift.restclient.ResourceKind
import com.openshift.restclient.authorization.TokenAuthorizationStrategy
import com.openshift.restclient.model.IPod
import java.io.File
import java.nio.file.Files
import java.util.*

class OpenshiftAgentMonitor(val labelMap: java.util.HashMap<String, String>): AgentMonitor {
    val client: IClient
    init {
        val openshiftUri = "https://${System.getenv("KUBERNETES_SERVICE_HOST")}:${System.getenv("KUBERNETES_SERVICE_PORT")}"
        client = ClientBuilder(openshiftUri).authorizationStrategy(TokenAuthorizationStrategy(getAuthenticationToken())).build()
    }

    override fun listAgents(): List<AgentInfo> {
        val pods = client.list<IPod>(ResourceKind.POD, getOpenshiftNamespace(), labelMap)
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
