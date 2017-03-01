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

import enmasse.discovery.DiscoveryClient
import enmasse.discovery.DiscoveryListener
import enmasse.discovery.Host
import io.vertx.core.Vertx
import java.util.*

class OpenshiftAgentMonitor(vertx: Vertx, val labelMap: java.util.HashMap<String, String>): AgentMonitor, DiscoveryListener {
    private val client: DiscoveryClient
    @Volatile private var currentAgents:List<AgentInfo> = emptyList()
    init {
        client = DiscoveryClient("podsense", labelMap, Optional.empty());
        client.addListener(this)
        vertx.deployVerticle(client)
    }

    override fun listAgents(): List<AgentInfo> {
        return currentAgents
    }

    override fun hostsChanged(hostSet: MutableSet<Host>) {
        currentAgents = hostSet.map { host -> AgentInfo(host.hostname, 8080) }
    }

}
