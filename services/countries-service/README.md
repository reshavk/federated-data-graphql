# GraphQL Countries Service - Phase 1

A production-ready GraphQL service built with Kotlin using `graphql-java` and Ktor. Fetches real country data from the public REST Countries API and exposes it through a unified GraphQL interface.

## Features

- ✅ **Open-Source Stack**: graphql-java (MIT) + Ktor (Apache 2.0)
- ✅ **External API Integration**: Calls REST Countries API to fetch real data
- ✅ **Federation-Ready Schema**: Designed for seamless Phase 2 multi-service federation
- ✅ **Production-Grade**: Health checks, comprehensive error handling, Kotlin coroutines
- ✅ **Docker-Ready**: Multi-stage Dockerfile, docker-compose for local testing
- ✅ **Type-Safe**: Full Kotlin type safety with Jackson JSON mapping

## Quick Start

### Prerequisites

- Java 21+ (or use [devcontainer](DEVELOPMENT.md) for consistent environment)
- Gradle 8.4+ (or use the wrapper `./gradlew`)
- Docker & Docker Compose (optional, for containerized setup)

### Local Development (Recommended)

**Option 1: Using Devcontainer (Best for consistency)**

See [DEVELOPMENT.md](DEVELOPMENT.md#option-1-using-devcontainer-recommended) for devcontainer setup with automatic environment configuration.

**Option 2: Direct Local Build**

```bash
# Build the project
./gradlew build

# Run the service
./gradlew run
```

The GraphQL server will start on `http://localhost:8080`

For detailed development setup, see [DEVELOPMENT.md](DEVELOPMENT.md).

## API Endpoints

### GraphQL Endpoint

**POST** `http://localhost:8080/graphql`

Send GraphQL queries and mutations in the request body:

```json
{
  "query": "query { countries(limit: 5) { cca2 name region } }"
}
```

### Health Checks

**GET** `http://localhost:8080/health` - Liveness probe  
**GET** `http://localhost:8080/ready` - Readiness probe  
**GET** `http://localhost:8080/` - Service info

## Sample Queries

### Get a Single Country by Code

```graphql
query {
  country(cca2: "US") {
    cca2
    cca3
    name
    officialName
    region
    subregion
    capital
    population
    area
    flag
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
```

### Get All Countries in a Region

```graphql
query {
  countries(region: "Europe", limit: 10) {
    cca2
    name
    capital
    population
    coordinates {
      latitude
      longitude
    }
  }
}
```

### Search Countries by Name

```graphql
query {
  searchCountries(name: "United", limit: 5) {
    cca2
    name
    region
  }
}
```

### Full Country Query with All Fields

```graphql
query {
  country(cca2: "FR") {
    cca2
    cca3
    name
    officialName
    region
    subregion
    capital
    languages {
      code
      name
    }
    currencies {
      code
      symbol
      name
    }
    continents
    area
    population
    timezones
    coordinates {
      latitude
      longitude
    }
    flag
    borders {
      cca2
      name
    }
  }
}
```

## Project Structure

```
graphql-countries-service/
├── src/
│   ├── main/
│   │   ├── kotlin/com/example/graphql/
│   │   │   ├── Main.kt                          # Application entry point
│   │   │   ├── client/
│   │   │   │   └── RestCountriesClient.kt       # REST API client
│   │   │   ├── models/
│   │   │   │   └── Models.kt                    # DTOs and domain models
│   │   │   ├── schema/
│   │   │   │   ├── Datafetchers.kt              # GraphQL datafetchers
│   │   │   │   └── SchemaProvider.kt            # Schema builder
│   │   │   └── server/
│   │   │       └── GraphQLServer.kt             # Ktor server setup
│   │   └── resources/
│   │       ├── graphql/
│   │       │   └── schema.graphql               # GraphQL schema
│   │       └── application.conf                 # Ktor configuration
│   └── test/
│       └── kotlin/
│           └── GraphQLIntegrationTests.kt       # Integration tests
├── build.gradle.kts                             # Gradle build config
├── settings.gradle.kts                          # Gradle settings
├── Dockerfile                                   # Multi-stage Docker build
├── docker-compose.yml                           # Docker Compose config
└── README.md                                    # This file
```

## Configuration

### Environment Variables

- `PORT` - Server port (default: `8080`)
- `REST_COUNTRIES_URL` - REST Countries API base URL (default: `https://restcountries.com/v3.1`)

Example:

```bash
PORT=9000 ./gradlew run
```

## Testing

Run integration tests:

```bash
./gradlew test
```

Tests use MockWebServer to mock REST Countries API responses, ensuring no external dependencies during testing.

## Building for Production

### Generate Fat JAR

```bash
./gradlew shadowJar
```

Output: `build/libs/graphql-countries-service-1.0.0.jar`

### Build Docker Image

```bash
docker build -t graphql-countries-service:1.0.0 .
```

### Run Docker Container

```bash
docker run -p 8080:8080 graphql-countries-service:1.0.0
```

## Architecture & Design

### Technology Stack

| Component       | Technology         | License    |
| --------------- | ------------------ | ---------- |
| GraphQL Engine  | graphql-java 25.0  | MIT        |
| Schema Tools    | graphql-java-tools | MIT        |
| HTTP Server     | Ktor 2.3.7         | Apache 2.0 |
| Language        | Kotlin 1.9.22      | Apache 2.0 |
| JSON Processing | Jackson 2.16.1     | Apache 2.0 |
| Runtime         | Java 21 LTS        | GPLv2+CPE  |

All libraries are open-source with permissive licenses suitable for production use.

### Data Flow

```
REST Countries API
       ↓
RestCountriesClient (HTTP calls, DTO mapping)
       ↓
Models (Country, Language, Currency, etc.)
       ↓
CountryDataFetchers (GraphQL resolution)
       ↓
SchemaProvider (builds executable schema)
       ↓
Ktor Server (/graphql endpoint)
       ↓
Client (receives unified GraphQL response)
```

### Federation Design (Phase 2 Ready)

This service is designed with federation in mind from the ground up:

- **Primary Key**: Country entity uses `cca2` (ISO 3166-1 alpha-2 code) as stable, immutable `@key`
- **Shareable Types**: Language and Currency are marked `@shareable` for Phase 2 reference
- **Entity Boundaries**: Clear separation between query-accessible data and federation-friendly entities
- **Extensible Schema**: Phase 2 services can reference and extend Country without redundancy

See [FEDERATION_DESIGN.md](FEDERATION_DESIGN.md) for detailed Phase 2 migration strategy.

## API Reliability

### Error Handling

- REST Countries API failures are handled gracefully (returns null or empty list)
- GraphQL errors are properly formatted with messages and stack traces
- Health check endpoint indicates service status

### Timeout & Circuit Breaking

- HTTP client timeout: 10 seconds (configurable)
- Graceful degradation on API failure
- No cascading failures

## Development Workflow

### Adding a New Query

1. Update `schema.graphql` with new Query type
2. Add corresponding datafetcher in `CountryDataFetchers.kt`
3. Register datafetcher in `SchemaProvider.kt`
4. Write tests in `GraphQLIntegrationTests.kt`

### Extending the Schema

1. Keep entity relationships federation-compatible
2. Mark shareable fields with `@shareable` directive
3. Use stable identifiers as `@key` fields
4. Document in FEDERATION_DESIGN.md

## Next Steps: Phase 2 Federation

When ready to build the second microservice:

1. Deploy this service as a federated subgraph
2. Build Phase 2 service with its own domain (e.g., Travel, Products)
3. Reference Country entities using `cca2` code
4. Use Apollo Gateway to compose unified schema

Detailed roadmap in [FEDERATION_DESIGN.md](FEDERATION_DESIGN.md).

## License

This project uses only open-source libraries. See `build.gradle.kts` for full dependency list and licenses.

## Contributing

For improvements, bug fixes, or feature suggestions, please follow the existing code style and add tests for new functionality.

## Support

For issues with REST Countries API, see: https://restcountries.com/

For graphql-java documentation: https://www.graphql-java.com/

For Ktor documentation: https://ktor.io/
