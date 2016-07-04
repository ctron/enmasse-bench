package enmasse.perf

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServer
import io.vertx.core.impl.FileResolver

/**
 * @author Ulf Lilleengen
 */
class RemoteCollector(val clients: List<Client>): MetricCollector {

    val vertx: Vertx
    val server: HttpServer

    init {
        System.setProperty(FileResolver.CACHE_DIR_BASE_PROP_NAME, "/tmp/vert.x")
        vertx = Vertx.vertx()
        server = vertx.createHttpServer()
    };

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

