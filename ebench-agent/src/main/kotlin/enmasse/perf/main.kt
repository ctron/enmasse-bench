package enmasse.perf

import org.apache.commons.cli.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

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
    options.addOption(createOption("r", "reportInterval", "Interval when reporting statistics"))
    options.addOption(createOption("o", "outputReporter", "Output reporter (stdout or collector)"))

    try {
        val cmd = parser.parse(options, args)
        val clients = Integer.parseInt(cmd.getOptionValue("c"))
        val hostname = cmd.getOptionValue("h")
        val port = Integer.parseInt(cmd.getOptionValue("p"))
        val address = cmd.getOptionValue("a")
        val msgSize = Integer.parseInt(cmd.getOptionValue("s"))
        val duration = Integer.parseInt(cmd.getOptionValue("d"))
        val printInterval = if (cmd.hasOption("r")) java.lang.Long.parseLong(cmd.getOptionValue("r")) else null
        val outputReporter = cmd.getOptionValue("o", "stdout")

        val reporter = if (outputReporter.equals("collector")) CollectorReporter() else StdoutReporter()
        runBenchmark(clients, hostname, port, address, msgSize, duration, printInterval, reporter)
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

fun collectResult(clients: List<Client>): MetricSnapshot {
    return clients.map(Client::snapshot)
            .foldRight(emptyMetricSnapshot, { a, b -> mergeSnapshots(a, b)})
}

fun runBenchmark(clients: Int, hostname: String, port: Int, address: String, msgSize: Int, duration: Int, printInterval: Long?, resultReporter: ResultReporter) {
    val clients = 1.rangeTo(clients).map { i ->
        Client(hostname, port, address, msgSize, duration)
    }

    val executor = Executors.newFixedThreadPool(clients.size)
    clients.forEach{c -> executor.execute(c)}
    executor.shutdown()

    // Timer is safe to use here, as we exit after the loop anyway
    val timer = Timer()
    if (printInterval != null) {
        val printIntervalMillis = TimeUnit.SECONDS.toMillis(printInterval)
        timer.schedule(timerTask {
            resultReporter.report(collectResult(clients))
        }, printIntervalMillis, printIntervalMillis)
    }
    executor.awaitTermination(duration + 10L, TimeUnit.SECONDS)
    timer.cancel()

    resultReporter.report(collectResult(clients))
}
