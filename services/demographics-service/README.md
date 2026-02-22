# Demographics GraphQL Service

A lightweight GraphQL microservice that provides demographic data for countries, built with Kotlin and GraphQL-Java.

## Overview

The Demographics Service is a GraphQL API that wraps the REST Countries API to provide population, area, population density, capitals, and timezone data for countries worldwide.

## Features

- **GraphQL API** - Query demographic data using GraphQL
- **RESTful REST Countries API** - Integrates with https://restcountries.com/v3.1
- **GraphiQL IDE** - Interactive GraphQL IDE at root path
- **Health Checks** - `/health` and `/ready` endpoints for service monitoring
- **Population Density Calculation** - Automatically calculated from population and area
- **Type Safety** - Kotlin with strong typing

## Architecture

```
┌─────────────────────────────┐
│   GraphQL Endpoint          │
│  (Port 8081, /graphql)      │
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│   SchemaProvider            │
│  - Loads GraphQL schema     │
│  - Builds RuntimeWiring     │
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│   Datafetchers              │
│  - Query resolvers          │
│  - Data transformation      │
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│   RestCountriesClient       │
│  - HTTP client to REST API  │
│  - Field limiting (10 max)  │
└──────────┬──────────────────┘
           │
┌──────────▼──────────────────┐
│   REST Countries API        │
│   https://restcountries.com │
└─────────────────────────────┘
```

## GraphQL Schema

### Query Type

```graphql
type Query {
  # Fetch all demographics data
  demographics(limit: Int = 50): [Demographics!]!
  
  # Fetch demographics for a specific country
  demographicsByCountry(cca2: String!): Demographics
  
  # Search demographics by country name
  searchDemographics(name: String!, limit: Int = 50): [Demographics!]!
}

type Demographics {
  cca2: String!                    # ISO 3166-1 alpha-2 code
  countryName: String!             # Common country name
  population: Int                  # Population count
  area: Float                      # Area in km²
  density: Float                   # Population per km²
  capitals: [String!]!             # List of capital cities
  timezones: [String!]!            # List of timezones
}
```

## Example Queries

### Get demographics for 5 countries
```graphql
query {
  demographics(limit: 5) {
    cca2
    countryName
    population
    area
    density
    capitals
    timezones
  }
}
```

### Get demographics for a specific country
```graphql
query {
  demographicsByCountry(cca2: "US") {
    countryName
    population
    area
    density
    capitals
    timezones
  }
}
```

### Search demographics by country name
```graphql
query {
  searchDemographics(name: "United", limit: 10) {
    cca2
    countryName
    population
    density
  }
}
```

## Building and Running

### Prerequisites
- Java 21 LTS
- Gradle 8.4
- Docker (optional)

### Local Build and Run

```bash
cd services/demographics-service

# Build
gradle build

# Run tests
gradle test

# Run the service
gradle run
```

Service will be available at `http://localhost:8081`

### Docker Build

```bash
# From workspace root
docker build -f services/demographics-service/Dockerfile -t demographics-service .

# Run container
docker run -p 8081:8081 demographics-service
```

### Docker Compose (Multi-Service)

```bash
# From workspace root
docker-compose up

# Countries Service: http://localhost:8080/graphql
# Demographics Service: http://localhost:8081/graphql
```

## REST Countries API Limitations

The REST Countries API enforces a **10-field maximum** per request. The Demographics Service uses these fields:

```
cca2, name, population, area, capital, timezones, latlng
```

This is optimized to stay within the API limit while providing all necessary demographic data.

## Technology Stack

- **Language**: Kotlin 1.9.23
- **Runtime**: Java 21 LTS
- **Web Framework**: Ktor 2.3.7 (CIO engine for async I/O)
- **GraphQL**: graphql-java 25.0
- **JSON Processing**: Jackson 2.16.1
- **HTTP Client**: Java 11+ HttpClient
- **Testing**: JUnit 5, Kotest, OKHttp MockWebServer
- **Logging**: SLF4J 2.0.11 with Logback 1.4.11
- **Container**: Alpine 3.18 + Java 21

## Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/graphql` | GET, POST | GraphQL API endpoint |
| `/` | GET | GraphiQL IDE |
| `/health` | GET | Service health status |
| `/ready` | GET | Service readiness probe |

## Development

### Project Structure

```
services/demographics-service/
├── build.gradle.kts           # Gradle configuration
├── settings.gradle.kts        # Project settings
├── Dockerfile                 # Docker multi-stage build
├── .gitignore
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/demographics/
│   │   │   ├── Main.kt                       # Application entry point
│   │   │   ├── client/
│   │   │   │   └── RestCountriesClient.kt   # REST API client
│   │   │   ├── models/
│   │   │   │   └── Models.kt                 # Data models
│   │   │   ├── schema/
│   │   │   │   ├── SchemaProvider.kt         # GraphQL schema provider
│   │   │   │   └── Datafetchers.kt           # Query resolvers
│   │   │   └── server/
│   │   │       └── GraphQLServer.kt          # Ktor server configuration
│   │   └── resources/
│   │       ├── graphql/
│   │       │   └── schema.graphql            # GraphQL type definitions
│   │       └── application.conf              # Application configuration
│   └── test/
│       └── kotlin/com/example/demographics/
│           └── DemographicsIntegrationTests.kt
└── README.md
```

### Adding New Features

1. Extend `schema.graphql` with new types/queries
2. Update `Models.kt` with corresponding Kotlin data classes
3. Implement resolver logic in `Datafetchers.kt`
4. Add HTTP client methods in `RestCountriesClient.kt` if needed
5. Write tests in `DemographicsIntegrationTests.kt`

## Monitoring

The service provides health and readiness endpoints:

```bash
# Health check
curl http://localhost:8081/health

# Readiness check
curl http://localhost:8081/ready
```

## Next Steps

- **Federation**: Add Apollo Federation directives for schema composition
- **Caching**: Implement response caching for frequently accessed data
- **Advanced Filtering**: Add more demographic query filters
- **Metrics**: Integrate Prometheus for monitoring

## Related Services

- **Countries Service** (Port 8080): Comprehensive country data (countries-service/)

## License

MIT
