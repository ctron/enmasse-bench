package enmasse.perf

import io.vertx.core.impl.FileResolver
import org.apache.commons.cli.*
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
    System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x")
    val parser = DefaultParser()
    val options = Options()
    options.addOption(createRequiredOption("c", "clients", "Number of clients"))
    options.addOption(createRequiredOption("h", "hostname", "Hostname of server"))
    options.addOption(createRequiredOption("p", "port", "Port to use on server"))
    options.addOption(createRequiredOption("a", "address", "Address to use for messages"))
    options.addOption(createRequiredOption("s", "messageSize", "Size of messages"))
    options.addOption(createRequiredOption("d", "duration", "Number of seconds to run test"))
    options.addOption(createOption("r", "reportInterval", "Interval when reporting statistics"))
    options.addOption(createOption("m", "mode", "Mode (standalone or collector)"))
    options.addOption(createOption("w", "waitTime", "Wait time between sending messages (in milliseconds)"))

    try {
        val cmd = parser.parse(options, args)
        val clients = Integer.parseInt(cmd.getOptionValue("c"))
        val hostname = cmd.getOptionValue("h")
        val port = Integer.parseInt(cmd.getOptionValue("p"))
        val address = cmd.getOptionValue("a")
        val msgSize = Integer.parseInt(cmd.getOptionValue("s"))
        val duration = Integer.parseInt(cmd.getOptionValue("d"))
        val printInterval = if (cmd.hasOption("r")) java.lang.Long.parseLong(cmd.getOptionValue("r")) else null
        val mode = cmd.getOptionValue("m", "standalone")
        val waitTime = if (cmd.hasOption("w")) Integer.parseInt(cmd.getOptionValue("w")) else null

        val clientHandles = 1.rangeTo(clients).map { i ->
            Client(hostname, port, address, msgSize, duration, waitTime)
        }
        val collector =
                if (mode.equals("collector")) RemoteCollector(clientHandles)
                else PrintCollector(clientHandles, printInterval)

        runBenchmark(clientHandles, duration, collector)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench", options)
        System.exit(1)
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

fun runBenchmark(clients: List<Client>, duration: Int, metricCollector: MetricCollector) {

    val executor = Executors.newFixedThreadPool(clients.size)
    clients.forEach{c -> executor.execute(c)}
    metricCollector.start()
    executor.shutdown()
    executor.awaitTermination(duration + 10L, TimeUnit.SECONDS)
    metricCollector.stop()
}
