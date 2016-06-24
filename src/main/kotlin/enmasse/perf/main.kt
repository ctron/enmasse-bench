package enmasse.perf

import org.apache.commons.cli.*

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
    options.addOption(createRequiredOption("r", "runLength", "Number of seconds to run test"))

    try {
        val cmd = parser.parse(options, args)
        val clients = Integer.parseInt(cmd.getOptionValue("c"))
        val hostname = cmd.getOptionValue("h")
        val port = Integer.parseInt(cmd.getOptionValue("p"))
        val address = cmd.getOptionValue("a")
        val msgSize = Integer.parseInt(cmd.getOptionValue("s"))
        val runLength = java.lang.Long.parseLong(cmd.getOptionValue("r"))

        runBenchmark(clients, hostname, port, address, msgSize, runLength)
    } catch (e: ParseException) {
        println("Unable to parse arguments: ${args}")
        val formatter = HelpFormatter()
        formatter.printHelp("enmasse-bench", options)
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

fun runBenchmark(clients: Int, hostname: String, port: Int, address: String, msgSize: Int, runLength: Long) {
    val clients = 1.rangeTo(clients).map { i ->
        println("Creating client with id ${i}")
        val client = Client(hostname, port, address, msgSize, runLength)
        client
    }

    clients.forEach(Client::run)
}
