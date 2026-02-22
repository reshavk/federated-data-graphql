# GraphQL Countries Service - Implementation Summary

## Project Overview

A production-ready GraphQL service built with **Kotlin** and **graphql-java** (open-source, MIT licensed) that exposes real country data from the REST Countries API through a unified GraphQL interface.

**Built with:**

- ✅ graphql-java 25.0 (MIT)
- ✅ Ktor 2.3.7 (Apache 2.0)
- ✅ Kotlin 1.9.23 (Apache 2.0)
- ✅ Java 21 LTS (OpenJDK)
- ✅ 100% open-source stack

## Architecture

### Service Components

```
REST Countries API (Public)
        ↓
RestCountriesClient (HTTP calls, JSON mapping)
        ↓
Kotlin Data Models (DTOs + Domain models)
        ↓
GraphQL Datafetchers (Query resolution)
        ↓
graphql-java Engine (Schema validation, execution)
        ↓
Ktor HTTP Server (/graphql, /health endpoints)
        ↓
GraphQL Clients
```

### Project Structure

```
graphql-countries-service/
├── .devcontainer/              # VS Code devcontainer setup
│   ├── devcontainer.json       # Container configuration
│   └── Dockerfile              # Dev environment image
├── src/
│   ├── main/kotlin/com/example/graphql/
│   │   ├── Main.kt             # Application entry point
│   │   ├── client/
│   │   │   └── RestCountriesClient.kt
│   │   ├── models/
│   │   │   └── Models.kt       # Data classes
│   │   ├── schema/
│   │   │   ├── Datafetchers.kt
│   │   │   └── SchemaProvider.kt
│   │   └── server/
│   │       └── GraphQLServer.kt
│   ├── main/resources/
│   │   ├── graphql/
│   │   │   └── schema.graphql
│   │   └── application.conf
│   └── test/kotlin/
│       └── GraphQLIntegrationTests.kt
├── build.gradle.kts            # Gradle build configuration
├── settings.gradle.kts
├── gradle/wrapper/
├── Dockerfile                  # Production Docker image
├── docker-compose.yml          # Local Docker orchestration
├── README.md                   # API documentation
├── DEVELOPMENT.md              # Development setup guide
├── FEDERATION_DESIGN.md        # Phase 2 federation roadmap
└── .gitignore
```

## Key Features

### 1. GraphQL Queries

Three main query types:

- `country(cca2: String!): Country` - Fetch single country by ISO code
- `countries(region: String, limit: Int): [Country!]!` - List countries with optional filtering
- `searchCountries(name: String!, limit: Int): [Country!]!` - Search by country name

### 2. Data Types

- **Country** - Primary entity with all geographic/demographic data
- **Language** - Official languages (shareable for Phase 2)
- **Currency** - Official currencies (shareable for Phase 2)
- **Coordinates** - Geographic coordinates

### 3. Federation-Ready Design

Schema designed from day one for Phase 2 federation:

- Country uses `cca2` (ISO code) as primary key
- Language and Currency marked as shareable entities
- Clear entity boundaries for multi-service composition
- Documented migration path in FEDERATION_DESIGN.md

### 4. Error Handling

- Graceful handling of REST Countries API failures
- Proper GraphQL error formatting
- Connection timeouts (10s configurable)
- Nullable field handling

### 5. Health Checks

- `/health` - Liveness probe (for Kubernetes/ECS)
- `/ready` - Readiness probe
- `/` - Service info endpoint

## Development Workflow

### Setup Options

1. **Devcontainer (Recommended)**
   - Automatic Java 21 environment
   - All tools pre-configured
   - Consistent across team
   - See DEVELOPMENT.md

2. **Local with Java 21**
   - Direct Gradle builds
   - Fast iteration
   - Requires Java 21 LTS

### Build Commands

```bash
# Full build with tests
gradle build

# Build without tests
gradle build -x test

# Run tests only
gradle test

# Run the service
gradle run

# Build executable JAR
gradle fatJar
```

## Testing

### Unit + Integration Tests

Located in [src/test/kotlin/](src/test/kotlin/)

- MockWebServer for REST Countries API mocking
- JUnit 5 test framework
- GraphQL query validation
- Error scenario testing

Run with:

```bash
gradle test
```

### Manual Testing

```bash
# Health check
curl http://localhost:8080/health

# Sample query
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ country(cca2: \"US\") { name region population } }"}'
```

## Deployment

### Docker Deployment

```bash
# Build image
docker build -t graphql-countries-service:1.0.0 .

# Run container
docker run -p 8080:8080 graphql-countries-service:1.0.0
```

### Docker Compose (Local)

```bash
docker-compose up
```

### Production (ECS/AWS)

For Phase 3, deploy to single ECS cluster:

- Task definition with this image
- Service discovery for inter-service communication
- ALB pointing to service on port 8080
- Autoscaling based on CPU/memory

## Phase 2 Roadmap

### Current State (Phase 1)

✅ Single GraphQL service calling external API  
✅ Federation-ready schema design  
✅ Production-grade error handling  
✅ Comprehensive testing  
✅ Docker support

### Phase 2: Multi-Service Federation

📋 Deploy as Apollo subgraph  
📋 Build Travel/eCommerce service  
📋 Set up Apollo Gateway  
📋 Compose unified schema  
📋 Test cross-service queries

See [FEDERATION_DESIGN.md](FEDERATION_DESIGN.md) for detailed Phase 2 plan.

### Phase 3: ECS Cluster Deployment

📋 Deploy both services to single ECS cluster  
📋 Configure service discovery  
📋 Set up load balancer  
📋 Add monitoring/logging  
📋 Document deployment process

## Technology Decisions

### Why graphql-java?

- ✅ Oldest & most battle-tested GraphQL implementation on JVM (10+ years)
- ✅ Pure open-source, zero corporate lock-in
- ✅ Excellent separation of concerns (core engine)
- ✅ Works with any HTTP framework
- ✅ Clear path to federation via Nadel (Phase 2)

### Why Ktor?

- ✅ Lightweight, async-first HTTP server
- ✅ Kotlin-native, idiomatic
- ✅ Apache 2.0 open-source
- ✅ Minimal dependencies
- ✅ Excellent for microservices

### Why Java 21 LTS?

- ✅ Latest LTS version (supported until 2029)
- ✅ Virtual threads (Project Loom) for better concurrency
- ✅ Record types for data classes
- ✅ Pattern matching improvements
- ✅ Wide adoption by enterprises

## Production Readiness Checklist

- [x] Proper error handling
- [x] Health check endpoints
- [x] Comprehensive logging
- [x] Integration tests
- [x] Docker support
- [x] Configuration via environment variables
- [x] Graceful degradation on API failures
- [x] Connection timeouts
- [x] Clean code structure
- [x] Documentation

## Known Limitations & Future Work

1. **Caching** - Currently stateless, could add Redis for REST Countries responses
2. **Rate Limiting** - Not implemented, could add per-IP limits
3. **Authentication** - No auth layer (add if needed for Phase 2)
4. **Metrics** - Basic logging only, could add Prometheus metrics
5. **Subscriptions** - GraphQL subscriptions not yet implemented

## Support & Resources

- **graphql-java**: https://www.graphql-java.com/
- **Ktor**: https://ktor.io/
- **REST Countries API**: https://restcountries.com/
- **GraphQL**: https://graphql.org/

## License

All dependencies are open-source with permissive licenses (MIT, Apache 2.0). See build.gradle.kts for full list.

---

**Status**: ✅ Phase 1 Complete - Ready for Phase 2 Federation  
**Last Updated**: February 15, 2026  
**Version**: 1.0.0
