package com.example.graphql.server

import com.example.graphql.client.RestCountriesClient
import com.example.graphql.schema.SchemaProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.GraphQL
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class GraphQLServer(private val port: Int = 8080) {
    private val logger = LoggerFactory.getLogger(GraphQLServer::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    private lateinit var graphQL: GraphQL

    fun configure(application: Application) {
        logger.info("Configuring GraphQL server on port $port")

        // Initialize GraphQL engine
        val restCountriesClient = RestCountriesClient()
        val schemaProvider = SchemaProvider(restCountriesClient)
        graphQL = schemaProvider.buildGraphQL()

        application.routing {
            // GraphQL IDE (GraphiQL)
            get("/graphql") {
                val graphiqlHtml =
                        """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <title>GraphiQL - GraphQL Countries Service</title>
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/graphiql@3.0.0/graphiql.min.css" />
                        <style>
                            body {
                                height: 100%;
                                margin: 0;
                                width: 100%;
                                overflow: hidden;
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                            }
                            #graphiql {
                                height: 100vh;
                            }
                            .header {
                                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                                color: white;
                                padding: 15px 20px;
                                text-align: center;
                            }
                            .header h1 {
                                margin: 0;
                                font-size: 20px;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="header">
                            <h1>🌍 GraphQL Countries Service - Interactive IDE</h1>
                        </div>
                        <div id="graphiql">Loading GraphiQL...</div>
                        <script crossorigin src="https://cdn.jsdelivr.net/npm/react@18.2.0/umd/react.production.min.js"></script>
                        <script crossorigin src="https://cdn.jsdelivr.net/npm/react-dom@18.2.0/umd/react-dom.production.min.js"></script>
                        <script src="https://cdn.jsdelivr.net/npm/graphiql@3.0.0/graphiql.min.js"></script>
                        <script src="https://cdn.jsdelivr.net/npm/@graphiql/plugin-docs@0.1.0/dist/graphiql-plugin-docs.umd.js"></script>
                        <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@graphiql/plugin-docs@0.1.0/dist/style.css" />
                        <script>
                            const root = ReactDOM.createRoot(document.getElementById('graphiql'));
                            const docsPlugin = window.GraphiQLPluginDocs?.docsPlugin?.() || null;
                            root.render(
                                React.createElement(GraphiQL, {
                                    fetcher: async (graphQLParams) => {
                                        const response = await fetch('/graphql', {
                                            method: 'post',
                                            headers: {'Content-Type': 'application/json'},
                                            body: JSON.stringify(graphQLParams)
                                        });
                                        return response.json();
                                    },
                                    defaultQuery: `# Welcome to GraphQL Countries Service!
#
# Try querying countries data:

query GetCountries {
  countries(limit: 5) {
    cca2
    name
    region
    population
  }
}`,
                                    plugins: docsPlugin ? [docsPlugin] : []
                                })
                            );
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                call.respondText(graphiqlHtml, ContentType.Text.Html)
            }

            // GraphQL endpoint
            post("/graphql") {
                try {
                    val body = call.receive<String>()
                    val request = objectMapper.readValue(body, GraphQLRequest::class.java)

                    logger.debug("Received GraphQL query: ${request.query}")

                    val result =
                            graphQL.execute { executionInput ->
                                executionInput
                                        .query(request.query)
                                        .variables(request.variables ?: emptyMap())
                                        .operationName(request.operationName)
                            }

                    val response =
                            GraphQLResponse(
                                    data = result.getData(),
                                    errors =
                                            result.errors.map { error ->
                                                val errorMap = mutableMapOf<String, Any>()
                                                errorMap["message"] =
                                                        error.message ?: "Unknown error"

                                                error.locations?.let { locations ->
                                                    errorMap["locations"] =
                                                            locations.map {
                                                                mapOf(
                                                                        "line" to it.line,
                                                                        "column" to it.column
                                                                )
                                                            }
                                                }

                                                error.path?.let { path -> errorMap["path"] = path }

                                                errorMap
                                            }
                            )

                    call.respond(HttpStatusCode.OK, objectMapper.writeValueAsString(response))
                } catch (e: Exception) {
                    logger.error("Error processing GraphQL request", e)
                    call.respond(
                            HttpStatusCode.BadRequest,
                            objectMapper.writeValueAsString(
                                    mapOf(
                                            "errors" to
                                                    listOf(
                                                            mapOf(
                                                                    "message" to
                                                                            (e.message
                                                                                    ?: "Unknown error")
                                                            )
                                                    )
                                    )
                            )
                    )
                }
            }

            // Health check endpoint
            get("/health") {
                logger.debug("Health check requested")
                call.respond(HttpStatusCode.OK, mapOf("status" to "UP"))
            }

            // Ready check endpoint
            get("/ready") {
                logger.debug("Readiness check requested")
                call.respond(HttpStatusCode.OK, mapOf("status" to "READY"))
            }

            // Root info
            get("/") {
                call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                                "service" to "GraphQL Countries Service",
                                "version" to "1.0.0",
                                "graphql_endpoint" to "/graphql",
                                "health_endpoint" to "/health"
                        )
                )
            }
        }

        logger.info("GraphQL server configured successfully")
    }
}

data class GraphQLRequest(
        val query: String,
        val operationName: String? = null,
        val variables: Map<String, Any>? = null
)

data class GraphQLResponse(val data: Any? = null, val errors: List<Map<String, Any>>? = null)
