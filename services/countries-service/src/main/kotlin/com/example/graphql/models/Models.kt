package com.example.graphql.models

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * DTO classes representing the REST Countries API response structure
 * These are mapped from JSON responses to internal data models
 */

data class CountryDto(
    @JsonProperty("cca2")
    val cca2: String,
    
    @JsonProperty("cca3")
    val cca3: String,
    
    @JsonProperty("name")
    val name: NameDto,
    
    @JsonProperty("region")
    val region: String,
    
    @JsonProperty("subregion")
    val subregion: String?,
    
    @JsonProperty("capital")
    val capital: List<String>?,
    
    @JsonProperty("languages")
    val languages: Map<String, String>?,
    
    @JsonProperty("currencies")
    val currencies: Map<String, CurrencyDto>?,
    
    @JsonProperty("borders")
    val borders: List<String>?,
    
    @JsonProperty("continents")
    val continents: List<String>?,
    
    @JsonProperty("latlng")
    val latlng: List<Double>?,
    
    @JsonProperty("area")
    val area: Float?,
    
    @JsonProperty("population")
    val population: Int?,
    
    @JsonProperty("timezones")
    val timezones: List<String>?,
    
    @JsonProperty("flag")
    val flag: String?
)

data class NameDto(
    @JsonProperty("common")
    val common: String,
    
    @JsonProperty("official")
    val official: String?,
    
    @JsonProperty("nativeName")
    val nativeName: Map<String, NativeNameDto>?
)

data class NativeNameDto(
    @JsonProperty("official")
    val official: String?,
    
    @JsonProperty("common")
    val common: String?
)

data class CurrencyDto(
    @JsonProperty("name")
    val name: String,
    
    @JsonProperty("symbol")
    val symbol: String?
)

/**
 * Internal model classes for GraphQL types
 */

data class Country(
    val cca2: String,
    val cca3: String,
    val name: String,
    val officialName: String?,
    val region: String,
    val subregion: String?,
    val capital: List<String>,
    val languages: List<Language>,
    val currencies: List<Currency>,
    val borders: List<Country>,
    val continents: List<String>,
    val coordinates: Coordinates?,
    val area: Float?,
    val population: Int?,
    val timezones: List<String>,
    val flag: String?
)

data class Language(
    val code: String,
    val name: String
)

data class Currency(
    val code: String,
    val symbol: String?,
    val name: String
)

data class Coordinates(
    val latitude: Float,
    val longitude: Float
)
