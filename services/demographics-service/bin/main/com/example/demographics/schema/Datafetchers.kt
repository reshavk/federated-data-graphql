package com.example.demographics.schema

import com.example.demographics.client.RestCountriesClient
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory

class Datafetchers(private val restCountriesClient: RestCountriesClient) {
    private val logger = LoggerFactory.getLogger(Datafetchers::class.java)
    
    fun getDemographics(env: DataFetchingEnvironment): List<Map<String, Any?>> {
        val limit = env.getArgument<Int?>("limit") ?: 50
        logger.debug("Fetching all demographics with limit: {}", limit)
        
        val results = restCountriesClient.getAllDemographics(limit)
        logger.info("Fetched {} demographics records", results.size)
        
        return results.map { demo ->
            mapOf(
                "cca2" to demo.cca2,
                "countryName" to demo.countryName,
                "population" to demo.population,
                "area" to demo.area,
                "density" to demo.density,
                "capitals" to demo.capitals,
                "timezones" to demo.timezones
            )
        }
    }
    
    fun getDemographicsByCountry(env: DataFetchingEnvironment): Map<String, Any?>? {
        val cca2 = env.getArgument<String>("cca2")
        logger.debug("Fetching demographics for country code: {}", cca2)
        
        val demo = restCountriesClient.getDemographicsByCountryCode(cca2)
        
        return if (demo != null) {
            logger.info("Found demographics for: {}", cca2)
            mapOf(
                "cca2" to demo.cca2,
                "countryName" to demo.countryName,
                "population" to demo.population,
                "area" to demo.area,
                "density" to demo.density,
                "capitals" to demo.capitals,
                "timezones" to demo.timezones
            )
        } else {
            logger.warn("No demographics found for country code: {}", cca2)
            null
        }
    }
    
    fun searchDemographics(env: DataFetchingEnvironment): List<Map<String, Any?>> {
        val name = env.getArgument<String>("name")
        val limit = env.getArgument<Int?>("limit") ?: 50
        logger.debug("Searching demographics by name: {} with limit: {}", name, limit)
        
        val results = restCountriesClient.searchDemographicsByName(name, limit)
        logger.info("Found {} demographics records for search term: {}", results.size, name)
        
        return results.map { demo ->
            mapOf(
                "cca2" to demo.cca2,
                "countryName" to demo.countryName,
                "population" to demo.population,
                "area" to demo.area,
                "density" to demo.density,
                "capitals" to demo.capitals,
                "timezones" to demo.timezones
            )
        }
    }
}
