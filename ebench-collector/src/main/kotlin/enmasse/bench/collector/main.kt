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

import enmasse.perf.MetricSnapshot
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import org.apache.commons.cli.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
fun main(args: Array<String>) {
    val parser = DefaultParser()
    val timer = Executors.newScheduledThreadPool(1)
    val options = Options()
    options.addOption(createRequiredOption("i", "interval", "Collection interval (in seconds)"))
    options.addOption(createOption("a", "agents", "Comma-separated list of agent <host>:<port> (default: auto-discover)"))

    try {
        val cmd = parser.parse(options, args)
        val interval = java.lang.Long.parseLong(cmd.getOptionValue("i"))
        val agentMonitor = if (cmd.hasOption("a")) StaticAgentMonitor(parseAgents(cmd.getOptionValue("a"))) else OpenshiftAgentMonitor()
        val vertx = Vertx.vertx()
        val collector = Collector(vertx, agentMonitor)
        timer.scheduleAtFixedRate(collector, interval, interval, TimeUnit.SECONDS)
        startServer(vertx, collector);
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench-collector", options)
        System.exit(1)
    }
}

fun startServer(vertx: Vertx, collector: Collector) {
    vertx.createHttpServer()
            .requestHandler({ request ->
                val snapshot = collector.latest()
                val response = request.response();
                if (snapshot != null) {
                    val headers = response.headers()
                    headers.set("Content-Type", "application/json")
                    response.write(formatSnapshotJson(snapshot))
                }
                response.setStatusCode(HttpResponseStatus.OK.code())
                response.end()
            })
            .listen(8080);
    }

fun  formatSnapshotJson(snapshot: Pair<Int, MetricSnapshot>): String {
    val json = JsonObject()
    val snap = snapshot.second
    json.put("clients", snapshot.first)
    json.put("duration", snap.duration)
    json.put("messages", snap.numMessages)
    json.put("throughput", snap.throughput())
    val latencies = JsonObject()
    latencies.put("avg", snap.averageLatency())
    latencies.put("min", snap.minLatency)
    latencies.put("max", snap.maxLatency)
    latencies.put("50p", snap.percentile(0.5))
    latencies.put("75p", snap.percentile(0.75))
    latencies.put("90p", snap.percentile(0.9))
    latencies.put("95p", snap.percentile(0.95))
    json.put("latencies", latencies)
    return json.encode()
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
