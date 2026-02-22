package com.example.demographics.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO classes representing the REST Countries API response structure
 * Simplified for demographics-specific data
 */

data class DemographicsDto(
    @JsonProperty("cca2")
    val cca2: String,
    
    @JsonProperty("name")
    val name: NameDto,
    
    @JsonProperty("population")
    val population: Int?,
    
    @JsonProperty("area")
    val area: Float?,
    
    @JsonProperty("capital")
    val capital: List<String>?,
    
    @JsonProperty("timezones")
    val timezones: List<String>?,
    
    @JsonProperty("latlng")
    val latlng: List<Double>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class NameDto(
    @JsonProperty("common")
    val common: String,
    
    @JsonProperty("official")
    val official: String?
)

/**
 * Domain model for Demographics GraphQL type
 */
data class Demographics(
    val cca2: String,
    val countryName: String,
    val population: Int?,
    val area: Float?,
    val density: Float?,
    val capitals: List<String>,
    val timezones: List<String>
) {
    companion object {
        fun from(dto: DemographicsDto): Demographics {
            val population = dto.population ?: 0
            val area = dto.area ?: 1.0f
            val density = if (area > 0) (population / area).toFloat() else null
            
            return Demographics(
                cca2 = dto.cca2,
                countryName = dto.name.common,
                population = population,
                area = area,
                density = density,
                capitals = dto.capital ?: emptyList(),
                timezones = dto.timezones ?: emptyList()
            )
        }
    }
}
