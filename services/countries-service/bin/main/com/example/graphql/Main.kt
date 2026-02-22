package com.example.graphql

import com.example.graphql.server.GraphQLServer
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("MainKt")

fun main() {
    logger.info("Starting GraphQL Countries Service")
    
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val server = GraphQLServer(port)
    
    embeddedServer(CIO, port = port) {
        server.configure(this)
    }.start(wait = true)
}
