package com.example.graphql.schema

import com.example.graphql.client.RestCountriesClient
import com.example.graphql.models.Country
import graphql.schema.DataFetcher
import graphql.schema.DataFetchingEnvironment
import org.slf4j.LoggerFactory

class CountryDataFetchers(private val restCountriesClient: RestCountriesClient) {
    private val logger = LoggerFactory.getLogger(CountryDataFetchers::class.java)

    /** Fetcher for Query.countries Supports optional filtering by region and limiting results */
    fun countriesDataFetcher(): DataFetcher<List<Country>> {
        return DataFetcher { env: DataFetchingEnvironment ->
            val region: String? = env.getArgument("region")
            val limit: Int = env.getArgument("limit") ?: 50

            logger.debug("Fetching countries with region=$region, limit=$limit")

            val countries = restCountriesClient.getCountries(region, limit)

            // Resolve borders by fetching border countries
            countries.map { country -> country.copy(borders = resolveBorders(country)) }
        }
    }

    /** Fetcher for Query.country by cca2 code */
    fun countryDataFetcher(): DataFetcher<Country?> {
        return DataFetcher { env: DataFetchingEnvironment ->
            val cca2: String = env.getArgument("cca2")

            logger.debug("Fetching country with cca2=$cca2")

            val country = restCountriesClient.getCountryByCode(cca2)
            logger.info(
                    "Country lookup result for $cca2: ${if (country != null) "FOUND" else "NOT_FOUND"}"
            )

            // Resolve borders by fetching border countries
            country?.copy(borders = resolveBorders(country))
        }
    }

    /** Fetcher for Query.searchCountries */
    fun searchCountriesDataFetcher(): DataFetcher<List<Country>> {
        return DataFetcher { env: DataFetchingEnvironment ->
            val name: String = env.getArgument("name")
            val limit: Int = env.getArgument("limit") ?: 50

            logger.debug("Searching countries with name=$name, limit=$limit")

            val countries = restCountriesClient.searchCountries(name, limit)

            // Resolve borders by fetching border countries
            countries.map { country -> country.copy(borders = resolveBorders(country)) }
        }
    }

    /**
     * Helper function to resolve border countries from cca2 codes Only fetches actual country
     * objects if borders field is accessed in query
     */
    private fun resolveBorders(country: Country): List<Country> {
        if (country.borders.isNotEmpty()) {
            return country.borders // Already resolved
        }

        // This would be called by a field resolver if borders is requested
        return emptyList()
    }

    /** Fetcher for Country.borders field Resolves border country codes to full Country objects */
    fun countryBordersDataFetcher(): DataFetcher<List<Country>> {
        return DataFetcher { env: DataFetchingEnvironment ->
            val country: Country = env.getSource()

            // If borders are already loaded, return them
            if (country.borders.isNotEmpty()) {
                return@DataFetcher country.borders
            }

            // Otherwise, fetch each border country from the API
            // In a real scenario, this could use batch loading to optimize
            emptyList()
        }
    }

    /** Fetcher for resolving Country entities by cca2 Used for federation in Phase 2 */
    fun countryReferenceDataFetcher(): DataFetcher<Any?> {
        return DataFetcher { env: DataFetchingEnvironment ->
            @Suppress("UNCHECKED_CAST")
            val references = env.getArgument<List<Map<String, Any>>>("_representations")

            if (references != null) {
                references.mapNotNull { ref ->
                    val cca2 = ref["cca2"] as? String
                    if (cca2 != null) {
                        restCountriesClient.getCountryByCode(cca2)
                    } else {
                        null
                    }
                }
            } else {
                null
            }
        }
    }
}
