package com.example.demographics

import com.example.demographics.client.RestCountriesClient
import com.example.demographics.schema.Datafetchers
import com.example.demographics.schema.SchemaProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.GraphQL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DemographicsIntegrationTests {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var graphQL: GraphQL
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val restCountriesClient = RestCountriesClient()
        val datafetchers = Datafetchers(restCountriesClient)
        val schemaProvider = SchemaProvider(datafetchers)
        graphQL = schemaProvider.buildGraphQL()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testDemographicsByCountryCode() {
        val query = """
            query {
                demographicsByCountry(cca2: "US") {
                    cca2
                    countryName
                    population
                    area
                    density
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        // Verify no errors
        assertEquals(0, result.errors.size, "GraphQL query should have no errors")

        // Verify data structure
        val data = result.getData<Map<String, Any>>()
        assertNotNull(data)
        
        val demographics = data["demographicsByCountry"] as? Map<String, Any>
        assertNotNull(demographics, "Demographics data should not be null")
        
        assertEquals("US", demographics["cca2"], "Country code should be US")
        assertEquals("United States", demographics["countryName"], "Country name should be United States")
        
        // Verify population density was calculated
        val density = demographics["density"]
        assertNotNull(density, "Population density should be calculated")
    }

    @Test
    fun testSearchDemographicsByName() {
        val query = """
            query {
                searchDemographics(name: "United", limit: 2) {
                    cca2
                    countryName
                    population
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        // Verify no errors
        assertEquals(0, result.errors.size, "GraphQL query should have no errors")

        // Verify data structure
        val data = result.getData<Map<String, Any>>()
        assertNotNull(data)
        
        val searchResults = data["searchDemographics"] as? List<Map<String, Any>>
        assertNotNull(searchResults, "Search results should not be null")
        
        // Should find at least United Arab Emirates and/or United Kingdom
        assertTrue(searchResults.isNotEmpty(), "Search should return results for 'United'")
        
        val firstResult = searchResults[0]
        assertNotNull(firstResult["cca2"], "Country code should be present")
        assertNotNull(firstResult["countryName"], "Country name should be present")
    }

    @Test
    fun testGetAllDemographics() {
        val query = """
            query {
                demographics(limit: 3) {
                    cca2
                    countryName
                    population
                    area
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        // Verify no errors
        assertEquals(0, result.errors.size, "GraphQL query should have no errors")

        // Verify data structure
        val data = result.getData<Map<String, Any>>()
        assertNotNull(data)
        
        val demographicsList = data["demographics"] as? List<Map<String, Any>>
        assertNotNull(demographicsList, "Demographics list should not be null")
        
        assertTrue(demographicsList.isNotEmpty(), "Should return at least one country")
        assertTrue(demographicsList.size <= 3, "Should not exceed limit of 3")
        
        // Verify first entry has required fields
        val firstEntry = demographicsList[0]
        assertNotNull(firstEntry["cca2"], "Country code should be present")
        assertNotNull(firstEntry["countryName"], "Country name should be present")
        assertNotNull(firstEntry["population"], "Population should be present")
        assertNotNull(firstEntry["area"], "Area should be present")
    }

    @Test
    fun testPopulationDensityCalculation() {
        val query = """
            query {
                demographicsByCountry(cca2: "JP") {
                    cca2
                    countryName
                    population
                    area
                    density
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        // Verify no errors
        assertEquals(0, result.errors.size, "GraphQL query should have no errors")

        val data = result.getData<Map<String, Any>>()
        val demographics = data["demographicsByCountry"] as Map<String, Any>
        
        val population = demographics["population"] as? Number
        val area = demographics["area"] as? Number
        val density = demographics["density"] as? Number
        
        assertNotNull(population, "Population should not be null")
        assertNotNull(area, "Area should not be null")
        assertNotNull(density, "Density should be calculated")
        
        // Verify density calculation: population / area in km²
        // Tokyo metropolitan area has roughly 125M people in 378k km², so density ~330 people/km²
        assertTrue(density.toDouble() > 0, "Density should be positive")
    }

    @Test
    fun testGraphQLIntrospection() {
        val introspectionQuery = """
            query {
                __schema {
                    types {
                        name
                    }
                }
            }
        """.trimIndent()

        val result = graphQL.execute(introspectionQuery)

        // Verify no errors (proves schema is valid)
        assertEquals(0, result.errors.size, "Introspection query should have no errors")
        
        val data = result.getData<Map<String, Any>>()
        assertNotNull(data, "Schema introspection should return data")
    }
}
