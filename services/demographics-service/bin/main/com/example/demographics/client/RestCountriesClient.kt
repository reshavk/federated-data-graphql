package com.example.demographics.client

import com.example.demographics.models.DemographicsDto
import com.example.demographics.models.Demographics
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.slf4j.LoggerFactory

class RestCountriesClient {
    private val logger = LoggerFactory.getLogger(RestCountriesClient::class.java)
    private val baseUrl = "https://restcountries.com/v3.1"
    private val mapper = ObjectMapper().registerKotlinModule()
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    // REST Countries API enforces maximum 10 fields per request
    private val fields = "cca2,name,population,area,capital,timezones,latlng"
    
    fun getDemographicsByCountryCode(code: String): Demographics? {
        return try {
            val url = "$baseUrl/alpha/$code?fields=$fields"
            logger.debug("GET request to: {}", url)
            
            val request = HttpRequest.newBuilder()
                .uri(URL(url).toURI())
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.debug("Response status: {}", response.statusCode())
            
            if (response.statusCode() == 200) {
                val dto = mapper.readValue(response.body(), DemographicsDto::class.java)
                Demographics.from(dto)
            } else {
                logger.warn("API returned status code: {}", response.statusCode())
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching demographics for code: {}", code, e)
            null
        }
    }
    
    fun getAllDemographics(limit: Int = 50): List<Demographics> {
        return try {
            val url = "$baseUrl/all?fields=$fields&limit=$limit"
            logger.debug("GET request to: {}", url)
            
            val request = HttpRequest.newBuilder()
                .uri(URL(url).toURI())
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.debug("Response status: {}", response.statusCode())
            
            if (response.statusCode() == 200) {
                val dtos = mapper.readValue(response.body(), Array<DemographicsDto>::class.java)
                dtos.take(limit).map { Demographics.from(it) }
            } else {
                logger.warn("API returned status code: {}", response.statusCode())
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error fetching all demographics", e)
            emptyList()
        }
    }
    
    fun searchDemographicsByName(name: String, limit: Int = 50): List<Demographics> {
        return try {
            val url = "$baseUrl/name/$name?fields=$fields&fullText=false"
            logger.debug("GET request to: {}", url)
            
            val request = HttpRequest.newBuilder()
                .uri(URL(url).toURI())
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build()
            
            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            
            logger.debug("Response status: {}", response.statusCode())
            
            if (response.statusCode() == 200) {
                val dtos = mapper.readValue(response.body(), Array<DemographicsDto>::class.java)
                dtos.take(limit).map { Demographics.from(it) }
            } else {
                logger.warn("API returned status code: {}", response.statusCode())
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error searching demographics by name: {}", name, e)
            emptyList()
        }
    }
}
