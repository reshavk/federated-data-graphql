package com.example.graphql.schema

import com.example.graphql.client.RestCountriesClient
import graphql.GraphQL
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import org.slf4j.LoggerFactory

class SchemaProvider(private val restCountriesClient: RestCountriesClient) {
    private val logger = LoggerFactory.getLogger(SchemaProvider::class.java)
    
    fun buildGraphQL(): GraphQL {
        val typeDefinitionRegistry = readSchemaFile()
        val runtimeWiring = buildRuntimeWiring()
        
        val executableSchema = SchemaGenerator().makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
        
        logger.info("GraphQL schema built successfully")
        
        return GraphQL.newGraphQL(executableSchema)
            .build()
    }
    
    private fun readSchemaFile(): graphql.schema.idl.TypeDefinitionRegistry {
        val schemaString = this::class.java.classLoader
            .getResourceAsStream("graphql/schema.graphql")
            ?.bufferedReader()
            ?.readText()
            ?: throw RuntimeException("Schema file not found: graphql/schema.graphql")
        
        return SchemaParser().parse(schemaString)
    }
    
    private fun buildRuntimeWiring(): RuntimeWiring {
        val fetchers = CountryDataFetchers(restCountriesClient)
        
        return RuntimeWiring.newRuntimeWiring()
            // Query resolvers
            .type("Query") { typeWiring ->
                typeWiring
                    .dataFetcher("countries", fetchers.countriesDataFetcher())
                    .dataFetcher("country", fetchers.countryDataFetcher())
                    .dataFetcher("searchCountries", fetchers.searchCountriesDataFetcher())
            }
            // Country field resolvers
            .type("Country") { typeWiring ->
                typeWiring
                    .dataFetcher("borders", fetchers.countryBordersDataFetcher())
            }
            .build()
    }
}
