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

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient

class OpenshiftAgentMonitor(val labelMap: Map<String, String>): AgentMonitor {
    private val client: KubernetesClient = DefaultKubernetesClient()

    override fun listAgents(): List<AgentInfo> {
        return client.pods().withLabels(labelMap).list().items
                .filter { pod -> pod.status.phase.equals("Running") && pod.status.podIP != null && pod.status.podIP != "" }
                .map { pod -> AgentInfo(pod.status.podIP, pod.spec.containers[0].ports[0].containerPort) }
    }
}
