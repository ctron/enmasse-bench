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

package enmasse.perf

import org.apache.commons.cli.*
import java.net.Inet4Address
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * This is a benchmarking tool designed to find the limits of an EnMasse cluster by doing request-response. The
 *
 * The tool can be used as a way of finding the
 * scalability properties of an EnMasse cluster, or as a performance stress tool to simulate many clients and
 * large amounts of messages.
 *
 * @author lulf
 */
fun main(args: Array<String>) {
    val parser = DefaultParser()
    val options = Options()
    options.addOption(createRequiredOption("a", "address", "Address to use for messages"))
    options.addOption(createRequiredOption("d", "duration", "Number of seconds to run test"))
    options.addOption(createOption("f", "format", "Output format(pretty, script or none. None means that the agent can be polled using http)"))
    options.addOption(createRequiredOption("h", "hosts", "<host>:<port> of server(s). If multiple servers are specified, senders and receivers will be assigned round robin"))
    options.addOption(createOption("i", "interval", "Interval when reporting statistics in pretty or script modes"))
    options.addOption(createRequiredOption("m", "messageSize", "Size of messages"))
    options.addOption(createOptionNoArg("p", "presettle", "Send presettled messages"))
    options.addOption(createRequiredOption("r", "receivers", "Number of receivers"))
    options.addOption(createRequiredOption("s", "senders", "Number of senders"))
    options.addOption(createOption("w", "waitTime", "Wait time between sending messages (in milliseconds)"))
    options.addOption(createOptionNoArg("c", "splitClients", "Attempt to force sender/receivers to different AMQP container endpoints by reconnecting"))
    options.addOption(createOptionNoArg("t", "topic", "Treat the address as a topic"))

    try {
        val cmd = parser.parse(options, args)
        val senders = Integer.parseInt(cmd.getOptionValue("s"))
        val receivers = Integer.parseInt(cmd.getOptionValue("r"))
        val hostnames:List<String> = parseHostNames(cmd.getOptionValue("h"))
        val address = cmd.getOptionValue("a")
        val msgSize = Integer.parseInt(cmd.getOptionValue("m"))
        val duration = Integer.parseInt(cmd.getOptionValue("d"))
        val printInterval = if (cmd.hasOption("i")) java.lang.Long.parseLong(cmd.getOptionValue("i")) else null
        val format = cmd.getOptionValue("f", "pretty")
        val waitTime = if (cmd.hasOption("w")) Integer.parseInt(cmd.getOptionValue("w")) else 0
        val presettled = cmd.hasOption("p")
        val splitClients = cmd.hasOption("c")
        val isTopic = cmd.hasOption("t")

        val clientId = Inet4Address.getLocalHost().hostName

        val senderIds:List<String> = 1.rangeTo(senders).map { i -> "${clientId}-sender-${i}" }
        val receiverIds:List<String> = 1.rangeTo(receivers).map { i -> "${clientId}-receiver-${i}" }

        val connectionMonitor = createConnectionMonitor(splitClients, senderIds.plus(receiverIds))
        var hostIt = hostnames.iterator()
        val senderHandles = senderIds.map { senderId ->
            if (!hostIt.hasNext()) {
                hostIt = hostnames.iterator()
            }
            var hostname = hostIt.next()
            Sender(senderId, hostname, address, isTopic, msgSize, duration, waitTime, presettled, connectionMonitor)
        }

        val receiverHandlers = receiverIds.map { receiverId ->
            if (!hostIt.hasNext()) {
                hostIt = hostnames.iterator()
            }
            var hostname = hostIt.next()
            Receiver(receiverId, hostname, address, isTopic, msgSize, duration, connectionMonitor)
        }
        val clientHandles = senderHandles.plus(receiverHandlers)

        val metricHandles = if (senderHandles.size > 0) senderHandles else receiverHandlers
        val collector =
                if (format.equals("none")) RemoteCollector(metricHandles)
                else if (format.equals("script")) TimedCollector(metricHandles, printInterval, ::printSnapshotScriptable)
                else TimedCollector(metricHandles, printInterval, ::printSnapshotPretty)

        Runtime.getRuntime().addShutdownHook(Thread(Runnable { collector.stop() }))
        runBenchmark(clientHandles, duration, collector)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench", options)
        System.exit(1)
    }
}

fun  parseHostNames(hostnames: String?): List<String> {
    if (hostnames == null) {
        throw IllegalArgumentException("Hostnames must be specified")
    }
    if (hostnames.contains(",")) {
        return hostnames.split(",")
    } else {
        return listOf(hostnames)
    }
}

private fun  createConnectionMonitor(splitClients: Boolean, clientIds: List<String>): ConnectionMonitor {
        if (splitClients) {
            return ClientSplitter(clientIds)
        } else {
            return DummyMonitor()
        }
    }

fun createOption(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
            .hasArg()
            .desc(desc)
            .build()
}

fun createOptionNoArg(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
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

fun runBenchmark(clients: List<Client>, duration: Int, metricCollector: MetricCollector) {

    val executor = Executors.newFixedThreadPool(clients.size)
    clients.forEach{c -> executor.execute(c)}
    metricCollector.start()
    executor.shutdown()
    executor.awaitTermination(duration + 10L, TimeUnit.SECONDS)
}
