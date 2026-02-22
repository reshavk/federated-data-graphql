package com.example.graphql

import com.example.graphql.client.RestCountriesClient
import com.example.graphql.schema.SchemaProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import graphql.GraphQL
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GraphQLIntegrationTests {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var graphQL: GraphQL
    private val objectMapper = ObjectMapper().registerKotlinModule()

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        val restCountriesClient = RestCountriesClient(baseUrl)
        val schemaProvider = SchemaProvider(restCountriesClient)
        graphQL = schemaProvider.buildGraphQL()
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testCountryQueryByCca2() {
        // Mock REST Countries API response (returns single object with fields parameter)
        val mockResponse =
                """
            {
                "cca2": "US",
                "cca3": "USA",
                "name": {
                    "common": "United States",
                    "official": "United States of America"
                },
                "region": "Americas",
                "subregion": "North America",
                "capital": ["Washington, D.C."],
                "languages": {
                    "eng": "English"
                },
                "currencies": {
                    "USD": {
                        "name": "United States dollar",
                        "symbol": "$"
                    }
                },
                "borders": ["CAN", "MEX"],
                "continents": ["North America"],
                "latlng": [37.0902, -95.7129],
                "area": 9833517.0,
                "population": 338289857,
                "timezones": ["UTC-12:00", "UTC-11:00", "UTC-10:00"],
                "flag": "🇺🇸"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(mockResponse))

        val query =
                """
            query {
                country(cca2: "US") {
                    cca2
                    name
                    region
                    capital
                    population
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        assertEquals(0, result.errors.size, "Query should not have errors")
        assertNotNull(result.getData())

        @Suppress("UNCHECKED_CAST") val data = result.getData<Map<String, Any?>>()
        val country = data["country"] as? Map<String, Any?>

        assertNotNull(country)
        assertEquals("US", country["cca2"])
        assertEquals("United States", country["name"])
        assertEquals("Americas", country["region"])
    }

    @Test
    fun testCountriesQuery() {
        val mockResponse =
                """
            [
                {
                    "cca2": "US",
                    "cca3": "USA",
                    "name": {
                        "common": "United States",
                        "official": "United States of America"
                    },
                    "region": "Americas",
                    "subregion": "North America",
                    "capital": ["Washington, D.C."],
                    "languages": {"eng": "English"},
                    "currencies": {"USD": {"name": "United States dollar", "symbol": "$"}},
                    "borders": [],
                    "continents": ["North America"],
                    "latlng": [37.0902, -95.7129],
                    "area": 9833517.0,
                    "population": 338289857,
                    "timezones": ["UTC-12:00"],
                    "flag": "🇺🇸"
                },
                {
                    "cca2": "CA",
                    "cca3": "CAN",
                    "name": {
                        "common": "Canada",
                        "official": "Canada"
                    },
                    "region": "Americas",
                    "subregion": "North America",
                    "capital": ["Ottawa"],
                    "languages": {"eng": "English", "fra": "French"},
                    "currencies": {"CAD": {"name": "Canadian dollar", "symbol": "$"}},
                    "borders": [],
                    "continents": ["North America"],
                    "latlng": [56.1304, -106.3468],
                    "area": 9984670.0,
                    "population": 38929902,
                    "timezones": ["UTC-07:00"],
                    "flag": "🇨🇦"
                }
            ]
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(mockResponse))

        val query =
                """
            query {
                countries(limit: 2) {
                    cca2
                    name
                    region
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        assertEquals(0, result.errors.size, "Query should not have errors")
        assertNotNull(result.getData())

        @Suppress("UNCHECKED_CAST") val data = result.getData<Map<String, Any?>>()
        val countries = data["countries"] as? List<Map<String, Any?>>

        assertNotNull(countries)
        assertTrue(countries.size >= 2)
    }

    @Test
    fun testCountryWithLanguagesAndCurrencies() {
        val mockResponse =
                """
            {
                "cca2": "FR",
                "cca3": "FRA",
                "name": {
                    "common": "France",
                    "official": "French Republic"
                },
                "region": "Europe",
                "subregion": "Western Europe",
                "capital": ["Paris"],
                "languages": {
                    "fra": "French"
                },
                "currencies": {
                    "EUR": {
                        "name": "Euro",
                        "symbol": "€"
                    }
                },
                "borders": ["AND", "BEL", "GER"],
                "continents": ["Europe"],
                "latlng": [46.2276, 2.2137],
                "area": 643801.0,
                "population": 67750000,
                "timezones": ["UTC+01:00"],
                "flag": "🇫🇷"
            }
        """.trimIndent()

        mockWebServer.enqueue(MockResponse().setBody(mockResponse))

        val query =
                """
            query {
                country(cca2: "FR") {
                    cca2
                    name
                    languages {
                        code
                        name
                    }
                    currencies {
                        code
                        symbol
                        name
                    }
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        assertEquals(0, result.errors.size, "Query should not have errors")
        assertNotNull(result.getData())

        @Suppress("UNCHECKED_CAST") val data = result.getData<Map<String, Any?>>()
        val country = data["country"] as? Map<String, Any?>

        assertNotNull(country)
        assertEquals("France", country["name"])

        @Suppress("UNCHECKED_CAST") val languages = country["languages"] as? List<Map<String, Any?>>
        assertNotNull(languages)
        assertTrue(languages.isNotEmpty())
        assertEquals("fra", languages[0]["code"])
        assertEquals("French", languages[0]["name"])

        @Suppress("UNCHECKED_CAST")
        val currencies = country["currencies"] as? List<Map<String, Any?>>
        assertNotNull(currencies)
        assertTrue(currencies.isNotEmpty())
        assertEquals("EUR", currencies[0]["code"])
        assertEquals("€", currencies[0]["symbol"])
        assertEquals("Euro", currencies[0]["name"])
    }

    @Test
    fun testCountryNotFound() {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))

        val query =
                """
            query {
                country(cca2: "ZZ") {
                    cca2
                    name
                }
            }
        """.trimIndent()

        val result = graphQL.execute(query)

        // Should return null without errors in GraphQL (API client handles errors gracefully)
        assertNotNull(result.getData())
        @Suppress("UNCHECKED_CAST") val data = result.getData<Map<String, Any?>>()
        assertEquals(null, data["country"])
    }
}
