package com.example.demographics.server

import com.example.demographics.client.RestCountriesClient
import com.example.demographics.schema.Datafetchers
import com.example.demographics.schema.SchemaProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.GraphQL
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class GraphQLServer(private val port: Int = 8081) {
    private val logger = LoggerFactory.getLogger(GraphQLServer::class.java)
    private val objectMapper = ObjectMapper().registerKotlinModule()
    
    private val restCountriesClient = RestCountriesClient()
    private val datafetchers = Datafetchers(restCountriesClient)
    private val schemaProvider = SchemaProvider(datafetchers)
    private val graphQL: GraphQL = schemaProvider.buildGraphQL()
    
    fun configure(application: Application) {
        logger.info("Configuring Demographics GraphQL Server on port {}", port)
        
        application.routing {
            // GraphQL IDE
            get("/graphql") {
                call.respondText(getGraphiQLHtml(), ContentType.Text.Html)
            }
            
            // GraphQL endpoint
            post("/graphql") {
                try {
                    val body = call.receive<String>()
                    val request = objectMapper.readValue(body, GraphQLRequest::class.java)
                    
                    logger.debug("Received GraphQL query: {}", request.query)
                    
                    val result = graphQL.execute { executionInput ->
                        executionInput
                            .query(request.query)
                            .variables(request.variables ?: emptyMap())
                            .operationName(request.operationName)
                    }
                    
                    if (result.errors.isNotEmpty()) {
                        logger.warn("GraphQL errors: {}", result.errors.map { it.message })
                    }
                    
                    val responseMap = mutableMapOf<String, Any?>()
                    responseMap["data"] = result.getData<Any>()
                    if (result.errors.isNotEmpty()) {
                        responseMap["errors"] = result.errors.map { mapOf("message" to it.message) }
                    }
                    
                    call.respondText(
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMap),
                        ContentType.Application.Json
                    )
                } catch (e: Exception) {
                    logger.error("Error handling GraphQL request", e)
                    call.respondText(
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                            mapOf("error" to "Internal server error", "message" to e.message)
                        ),
                        ContentType.Application.Json,
                        HttpStatusCode.InternalServerError
                    )
                }
            }
            
            // Health check
            get("/health") {
                call.respondText(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        mapOf("status" to "UP")
                    ),
                    ContentType.Application.Json
                )
            }
            
            // Ready check
            get("/ready") {
                call.respondText(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        mapOf("ready" to true)
                    ),
                    ContentType.Application.Json
                )
            }
            
            // Root info
            get("/") {
                call.respondText(
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        mapOf(
                            "service" to "GraphQL Demographics Service",
                            "version" to "1.0.0",
                            "graphql_endpoint" to "/graphql",
                            "health_endpoint" to "/health"
                        )
                    ),
                    ContentType.Application.Json
                )
            }
        }
        
        logger.info("Demographics GraphQL Server configured successfully")
    }
    
    private fun getGraphiQLHtml(): String {
        return """
            <!DOCTYPE html>
            <html>
              <head>
                <style>
                  body {
                    height: 100%;
                    margin: 0;
                    width: 100%;
                    overflow: hidden;
                  }
                  #graphiql {
                    height: 100vh;
                  }
                </style>
                <script
                  crossorigin
                  src="https://unpkg.com/react@17/umd/react.production.min.js"
                ></script>
                <script
                  crossorigin
                  src="https://unpkg.com/react-dom@17/umd/react-dom.production.min.js"
                ></script>
                <link rel="stylesheet" href="https://unpkg.com/graphiql@2/graphiql.min.css" />
              </head>
              <body>
                <div id="graphiql">Loading...</div>
                <script
                  src="https://unpkg.com/graphiql@2/graphiql.min.js"
                  type="application/javascript"
                ></script>
                <script>
                  ReactDOM.render(
                    React.createElement(GraphiQL, {
                      fetcher: GraphiQL.createFetcher({ url: '/graphql' }),
                      defaultQuery: `# Demographics Service\n# Query all demographics data\nquery getDemographics {\n  demographics(limit: 5) {\n    cca2\n    countryName\n    population\n    area\n    density\n    capitals\n    timezones\n  }\n}`,
                    }),
                    document.getElementById('graphiql'),
                  );
                </script>
              </body>
            </html>
        """.trimIndent()
    }
    
    data class GraphQLRequest(
        val query: String,
        val operationName: String? = null,
        val variables: Map<String, Any>? = null
    )
}
