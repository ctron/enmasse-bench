package enmasse.perf

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
    val parser = DefaultParser()
    val options = Options()
    options.addOption(createRequiredOption("c", "clients", "Number of clients"))
    options.addOption(createRequiredOption("h", "hostname", "Hostname of server"))
    options.addOption(createRequiredOption("p", "port", "Port to use on server"))
    options.addOption(createRequiredOption("a", "address", "Address to use for messages"))
    options.addOption(createRequiredOption("s", "messageSize", "Size of messages"))
    options.addOption(createRequiredOption("d", "duration", "Number of seconds to run test"))

    try {
        val cmd = parser.parse(options, args)
        val clients = Integer.parseInt(cmd.getOptionValue("c"))
        val hostname = cmd.getOptionValue("h")
        val port = Integer.parseInt(cmd.getOptionValue("p"))
        val address = cmd.getOptionValue("a")
        val msgSize = Integer.parseInt(cmd.getOptionValue("s"))
        val duration = Integer.parseInt(cmd.getOptionValue("d"))

        runBenchmark(clients, hostname, port, address, msgSize, duration)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("ebench", options)
        System.exit(1)
    }
}

fun createRequiredOption(name: String, longName: String, desc: String): Option {
    return Option.builder(name).longOpt(longName)
            .hasArg()
            .desc(desc)
            .required()
            .build()
}

fun runBenchmark(clients: Int, hostname: String, port: Int, address: String, msgSize: Int, duration: Int) {
    val clients = 1.rangeTo(clients).map { i ->
        Client(hostname, port, address, msgSize, duration)
    }

    val executor = Executors.newFixedThreadPool(clients.size)
    clients.forEach{c -> executor.execute(c)}
    executor.shutdown()
    executor.awaitTermination(duration + 10L, TimeUnit.SECONDS)

    val results = clients.map(Client::result).foldRight(Result(0, 0), {a, b -> Result(a.numMessages + b.numMessages, Math.max(a.duration, b.duration)) })
    println("Sent and received ${results.numMessages} in ${results.duration / 1000} seconds. Throughput: ${results.throughput()}")
}
