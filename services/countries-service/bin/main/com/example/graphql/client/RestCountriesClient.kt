package com.example.graphql.client

import com.example.graphql.models.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import org.slf4j.LoggerFactory

class RestCountriesClient(
        private val baseUrl: String = "https://restcountries.com/v3.1",
        private val timeout: Duration = Duration.ofSeconds(10)
) {
    private val logger = LoggerFactory.getLogger(RestCountriesClient::class.java)
    private val httpClient = HttpClient.newBuilder().connectTimeout(timeout).build()

    private val objectMapper = ObjectMapper().registerKotlinModule()

    /** Fetch a single country by cca2 code */
    fun getCountryByCode(cca2: String): Country? {
        return try {
            // Fields to request from REST API - limited to 10 max
            val fields =
                    "cca2,cca3,name,region,subregion,languages,currencies,continents,latlng,population"
            val url = "$baseUrl/alpha/$cca2?fields=$fields"
            logger.debug("Fetching country from: $url")
            val request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(timeout)
                            .GET()
                            .header("Accept", "application/json")
                            .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            logger.debug(
                    "Response status: ${response.statusCode()}, body length: ${response.body().length}"
            )

            if (response.statusCode() == 200) {
                try {
                    // With fields parameter, the /alpha endpoint returns a single object, not an
                    // array
                    val dto = objectMapper.readValue(response.body(), CountryDto::class.java)

                    val country = dtoToCountry(dto)
                    logger.info(
                            "Successfully fetched country: $cca2 -> ${country?.name ?: "conversion failed"}"
                    )
                    country
                } catch (e: Exception) {
                    logger.error(
                            "Failed to deserialize country response for $cca2: ${e.message}",
                            e
                    )
                    null
                }
            } else {
                logger.warn("Country not found: $cca2 (Status: ${response.statusCode()})")
                null
            }
        } catch (e: Exception) {
            logger.error("Error fetching country $cca2", e)
            null
        }
    }

    /** Fetch all countries or filter by region Note: REST API limits to 10 fields maximum */
    fun getCountries(region: String? = null, limit: Int = 50): List<Country> {
        return try {
            // Fields to request from REST API - limited to 10 max
            val fields =
                    "cca2,cca3,name,region,subregion,languages,currencies,continents,latlng,population"

            val baseUrlWithFields =
                    if (region != null) {
                        "$baseUrl/region/$region?fields=$fields"
                    } else {
                        "$baseUrl/all?fields=$fields"
                    }

            logger.debug("Fetching countries from: $baseUrlWithFields")
            val request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(baseUrlWithFields))
                            .timeout(timeout)
                            .GET()
                            .header("Accept", "application/json")
                            .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            logger.debug("Countries response status: ${response.statusCode()}")

            if (response.statusCode() == 200) {
                @Suppress("UNCHECKED_CAST")
                val dtos =
                        objectMapper.readValue(
                                response.body(),
                                objectMapper.typeFactory.constructCollectionType(
                                        List::class.java,
                                        CountryDto::class.java
                                )
                        ) as
                                List<CountryDto>
                logger.debug("Deserialized ${dtos.size} countries from API")
                dtos.mapNotNull { dto -> dtoToCountry(dto) }.take(limit)
            } else {
                logger.warn("Failed to fetch countries (Status: ${response.statusCode()})")
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error fetching countries", e)
            emptyList()
        }
    }

    /** Search countries by name Note: REST API limits to 10 fields maximum */
    fun searchCountries(name: String, limit: Int = 50): List<Country> {
        return try {
            val fields =
                    "cca2,cca3,name,region,subregion,languages,currencies,continents,latlng,population"
            val url = "$baseUrl/name/$name?fields=$fields"

            logger.debug("Searching countries from: $url")
            val request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .timeout(timeout)
                            .GET()
                            .header("Accept", "application/json")
                            .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            logger.debug("Search response status: ${response.statusCode()}")

            if (response.statusCode() == 200) {
                @Suppress("UNCHECKED_CAST")
                val dtos =
                        objectMapper.readValue(
                                response.body(),
                                objectMapper.typeFactory.constructCollectionType(
                                        List::class.java,
                                        CountryDto::class.java
                                )
                        ) as
                                List<CountryDto>
                logger.debug("Found ${dtos.size} countries matching: $name")
                dtos.mapNotNull { dto -> dtoToCountry(dto) }.take(limit)
            } else {
                logger.warn(
                        "No countries found with name: $name (Status: ${response.statusCode()})"
                )
                emptyList()
            }
        } catch (e: Exception) {
            logger.error("Error searching countries by name: $name", e)
            emptyList()
        }
    }

    /** Convert DTO from REST API to internal Country model */
    private fun dtoToCountry(dto: CountryDto): Country? {
        return try {
            val languages =
                    dto.languages?.map { (code, name) -> Language(code, name) } ?: emptyList()

            val currencies =
                    dto.currencies?.map { (code, currencyDto) ->
                        Currency(code, currencyDto.symbol, currencyDto.name)
                    }
                            ?: emptyList()

            val coordinates =
                    if (dto.latlng != null && dto.latlng.size >= 2) {
                        try {
                            Coordinates(
                                    latitude = dto.latlng[0].toFloat(),
                                    longitude = dto.latlng[1].toFloat()
                            )
                        } catch (e: Exception) {
                            logger.warn("Failed to parse coordinates for ${dto.cca2}: ${e.message}")
                            null
                        }
                    } else {
                        null
                    }

            Country(
                    cca2 = dto.cca2,
                    cca3 = dto.cca3,
                    name = dto.name.common,
                    officialName = dto.name.official,
                    region = dto.region,
                    subregion = dto.subregion,
                    capital = dto.capital ?: emptyList(),
                    languages = languages,
                    currencies = currencies,
                    borders = emptyList(), // Will be resolved separately
                    continents = dto.continents ?: emptyList(),
                    coordinates = coordinates,
                    area = dto.area,
                    population = dto.population,
                    timezones = dto.timezones ?: emptyList(),
                    flag = dto.flag
            )
        } catch (e: Exception) {
            logger.error("Error converting DTO to Country: ${e.message}", e)
            null
        }
    }
}
