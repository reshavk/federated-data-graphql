package com.example.demographics.schema

import graphql.GraphQL
import graphql.schema.GraphQLSchema
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.slf4j.LoggerFactory

class SchemaProvider(private val datafetchers: Datafetchers) {
    private val logger = LoggerFactory.getLogger(SchemaProvider::class.java)
    
    fun buildGraphQL(): GraphQL {
        logger.info("Building GraphQL schema for Demographics Service")
        
        val schemaText = javaClass.classLoader.getResource("graphql/schema.graphql")
            ?.readText()
            ?: throw IllegalStateException("Could not find schema.graphql")
        
        val typeDefinitionRegistry = SchemaParser().parse(schemaText)
        val runtimeWiring = buildRuntimeWiring()
        val graphQLSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        
        logger.info("GraphQL schema built successfully")
        return GraphQL.newGraphQL(graphQLSchema).build()
    }
    
    private fun buildRuntimeWiring(): RuntimeWiring {
        return RuntimeWiring.newRuntimeWiring()
            .type("Query") { typeWiring ->
                typeWiring
                    .dataFetcher("demographics") { env -> datafetchers.getDemographics(env) }
                    .dataFetcher("demographicsByCountry") { env -> datafetchers.getDemographicsByCountry(env) }
                    .dataFetcher("searchDemographics") { env -> datafetchers.searchDemographics(env) }
            }
            .build()
    }
}
