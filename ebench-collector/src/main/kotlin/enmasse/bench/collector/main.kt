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

import io.vertx.core.impl.FileResolver
import org.apache.commons.cli.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author Ulf Lilleengen
 */
fun main(args: Array<String>) {
    System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x")
    val parser = DefaultParser()
    val timer = Executors.newScheduledThreadPool(1)
    val options = Options()
    options.addOption(createRequiredOption("i", "interval", "Collection interval (in seconds)"))
    options.addOption(createOption("a", "agents", "Comma-separated list of agent <host>:<port> (default: auto-discover)"))

    try {
        val cmd = parser.parse(options, args)
        val interval = java.lang.Long.parseLong(cmd.getOptionValue("i"))
        val agentMonitor = if (cmd.hasOption("a")) StaticAgentMonitor(parseAgents(cmd.getOptionValue("a"))) else OpenshiftAgentMonitor()
        timer.scheduleAtFixedRate(Collector(agentMonitor), interval, interval, TimeUnit.SECONDS)
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
