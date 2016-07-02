package enmasse.perf

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.io.StringWriter

/**
 * @author Ulf Lilleengen
 */
class RemoteCollector(val clients: List<Client>): MetricCollector {

    val vertx = Vertx.vertx()
    val server = vertx.createHttpServer()

    override fun start() {
        server.requestHandler { request ->
            val snapshot = collectResult(clients)
            val response = request.response()
            println("Duration is: " + snapshot.duration)
            printSnapshot(snapshot)
            response.headers().add("Content-Type", "application/json")
            val buffer = Buffer.buffer()
            serializeMetricSnapshot(buffer, snapshot)
            response.end(buffer)
        }.listen(8080)
    }

    override fun stop() {
        server.close()
        vertx.close()
    }

}

