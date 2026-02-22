# Federated GraphQL Services

A multi-microservice GraphQL architecture built with Kotlin/JVM, featuring independent GraphQL services that can be federated together using Apollo Federation.

**Status**: Pre-Federation (Phase 1) - Services are independent and fully functional. Federation setup coming in Phase 2.

## 📋 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    Apollo Gateway (Phase 2)                      │
│                  (Schema Composition & Routing)                  │
└────┬────────────────────────────────────┬────────────────────┬──┘
     │                                    │                    │
     ▼                                    ▼                    ▼
┌──────────────────┐           ┌──────────────────┐    ┌──────────────┐
│  Countries       │           │  Demographics    │    │  Additional  │
│  Service         │           │  Service         │    │  Services... │
│  (Port 8080)     │           │  (Port 8081)     │    │              │
└──────────────────┘           └──────────────────┘    └──────────────┘
│ Countries        │           │ Demographics     │
│ - Basic info     │           │ - Population     │
│ - Borders        │           │ - Area/Density   │
│ - Languages      │           │ - Capitals       │
│ - Currencies     │           │ - Timezones      │
│ - Coordinates    │           │                  │
│ - etc...         │           │                  │
└──────────────────┘           └──────────────────┘
        │                              │
        └──────────┬──────────────────┘
                   │
                   ▼
        ┌──────────────────────┐
        │ REST Countries API   │
        │ v3.1                 │
        │ https://restcountries│
        │  .com/v3.1           │
        └──────────────────────┘
```

## 🚀 Quick Start

### Prerequisites

- Java 21 LTS
- Gradle 8.4
- Docker & Docker Compose (optional)

### Build All Services

```bash
# Build countries-service
cd services/countries-service
gradle build

# Build demographics-service
cd ../demographics-service
gradle build

# Return to root
cd ../..
```

### Run Individual Services

#### Countries Service
```bash
cd services/countries-service
gradle run
# Service available at http://localhost:8080/graphql
```

#### Demographics Service
```bash
cd services/demographics-service
gradle run
# Service available at http://localhost:8081/graphql
```

### Run All Services with Docker Compose

```bash
docker-compose up
```

Both services will start:
- **Countries**: http://localhost:8080/graphql
- **Demographics**: http://localhost:8081/graphql

## 📚 Services

### 1. Countries Service
Complete country information service wrapping the REST Countries API.

**GraphQL Queries:**
- `countries(region, limit)`: List all countries with optional filtering
- `country(cca2)`: Get a specific country by ISO code
- `searchCountries(name, limit)`: Search countries by name

**GraphQL Types:** Country, Language, Currency, Coordinates

📖 **Documentation**: See [services/countries-service/README.md](services/countries-service/README.md)

**Status**: ✅ Production-ready (4/4 tests passing)

### 2. Demographics Service
Population, density, capitals, and timezone data for each country.

**GraphQL Queries:**
- `demographics(limit)`: List all demographics data
- `demographicsByCountry(cca2)`: Get demographics for a specific country
- `searchDemographics(name, limit)`: Search demographics by country name

**GraphQL Types:** Demographics

📖 **Documentation**: See [services/demographics-service/README.md](services/demographics-service/README.md)

**Status**: ✅ Production-ready (build successful, tests passing)

## 🏗️ Project Structure

```
.
├── .devcontainer/              # VS Code dev container configuration
│   ├── Dockerfile              # Dev environment Docker image
│   ├── devcontainer.json       # Dev container settings
│   └── .env                    # Environment variables
├── docker-compose.yml          # Multi-service orchestration
├── services/
│   ├── countries-service/      # GraphQL Countries Microservice
│   │   ├── src/main/kotlin/
│   │   ├── src/test/kotlin/
│   │   ├── build.gradle.kts
│   │   ├── Dockerfile
│   │   └── README.md
│   └── demographics-service/   # GraphQL Demographics Microservice
│       ├── src/main/kotlin/
│       ├── src/test/kotlin/
│       ├── build.gradle.kts
│       ├── Dockerfile
│       └── README.md
└── README.md                   # This file

```

## 🔧 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Language** | Kotlin | 1.9.23 |
| **Runtime** | Java | 21 LTS |
| **Web Framework** | Ktor | 2.3.7 |
| **GraphQL Engine** | graphql-java | 25.0 |
| **JSON Processing** | Jackson | 2.16.1 |
| **HTTP Client** | Java HttpClient | 11+ |
| **Testing** | JUnit 5, Kotest | 5.10.1, 5.8.0 |
| **Logging** | SLF4J + Logback | 2.0.11, 1.4.11 |
| **Container Runtime** | Alpine Linux + Java | 3.18 + 21-jdk |
| **Container Orchestration** | Docker Compose | 3.8 |

## 🧪 Testing

### Countries Service Tests
```bash
cd services/countries-service
gradle test
# 4 integration tests validating REST API integration
```

### Demographics Service Tests
```bash
cd services/demographics-service
gradle test
# Basic service structure validation
```

### Full Stack Testing
```bash
# Run all tests in workspace
gradle test -p services/countries-service
gradle test -p services/demographics-service
```

## 📝 API Examples

### GraphiQL IDE

Both services include GraphiQL interactive IDE:
- Countries: http://localhost:8080/
- Demographics: http://localhost:8081/

### Example: Query Countries

```graphql
query GetCountries {
  countries(limit: 3) {
    cca2
    name
    region
    population
    capital
  }
}
```

### Example: Get Specific Country

```graphql
query GetUSADemographics {
  country(cca2: "US") {
    name
    population
    area
    currencies {
      code
      symbol
      name
    }
  }
}
```

### Example: Demographics Lookup

```graphql
query GetDensity {
  demographicsByCountry(cca2: "JP") {
    countryName
    population
    area
    density
    timezones
  }
}
```

## 🔄 REST Countries API Integration

Both microservices integrate with the [REST Countries API v3.1](https://restcountries.com/v3.1).

**Important Constraint**: API enforces a **10-field maximum** per request.

**Field Selection Strategy**:
- **Countries Service**: `cca2,cca3,name,region,subregion,languages,currencies,continents,latlng,population`
- **Demographics Service**: `cca2,name,population,area,capital,timezones,latlng`

This constraint is handled transparently by the client classes.

## ☁️ Deployment

### Docker Build

Build individual services:
```bash
docker build -f services/countries-service/Dockerfile -t countries-service .
docker build -f services/demographics-service/Dockerfile -t demographics-service .
```

Run individual containers:
```bash
docker run -p 8080:8080 countries-service
docker run -p 8081:8081 demographics-service
```

### Docker Compose (Recommended)

```bash
docker-compose up -d

# Check service health
curl http://localhost:8080/health
curl http://localhost:8081/health

# Stop services
docker-compose down
```

## 🛠️ Development Workflow

### Using VS Code Dev Container

1. Open workspace in VS Code
2. When prompted, "Reopen in Container"
3. Container provides:
   - Java 21 JDK
   - Gradle 8.4
   - Kotlin 1.9.23
   - All necessary dev tools

### Local Development

1. Install Java 21 LTS
2. Install Gradle 8.4
3. Open in your favorite IDE
4. Run services individually with `gradle run`

### Adding a New Service

1. Create directory: `services/your-service/`
2. Copy structure from countries-service or demographics-service
3. Customize GraphQL schema and resolvers
4. Add Dockerfile for containerization
5. Update docker-compose.yml with new service
6. Update root README.md

## 📈 Next Steps (Phase 2 - Federation)

### Apollo Federation Setup
- [ ] Add federation directives to schema
- [ ] Implement entity resolvers
- [ ] Configure subgraph mode

```graphql
# Example federation directive (Phase 2)
extend schema
@link(url: "https://specs.apollo.dev/federation/v2.0")

type Country @key(fields: "cca2") {
  cca2: String!
  # ... other fields
}
```

### Apollo Gateway Implementation
- [ ] Create gateway service
- [ ] Configure subgraph routing
- [ ] Implement entity reference resolution
- [ ] Test federated queries

### Example Federated Query (Phase 2)
```graphql
query GetCountryAndDemographics {
  country(cca2: "US") {
    name
    region
    # Resolved from countries-service
    
    # Resolved from demographics-service via federation
    demographics {
      population
      density
      capitals
    }
  }
}
```

## 🐛 Troubleshooting

### Build Issues

**Problem**: Gradle dependency resolution fails
```bash
# Solution: Clear gradle cache
rm -rf ~/.gradle
gradle clean build
```

### Port Conflicts

**Problem**: Port 8080 or 8081 already in use
```bash
# Solution: Specify PORT environment variable
PORT=9000 gradle run  # for countries-service
PORT=9001 gradle run  # for demographics-service
```

### Container Build Issues

**Problem**: Docker build fails with Java compilation errors
```bash
# Solution: Clean rebuild with no cache
docker-compose build --no-cache
docker-compose up
```

## 📊 Service Health Monitoring

Each service provides health check endpoints:

```bash
# Countries Service
curl http://localhost:8080/health    # Health status
curl http://localhost:8080/ready     # Readiness probe

# Demographics Service
curl http://localhost:8081/health    # Health status
curl http://localhost:8081/ready     # Readiness probe
```

## 📚 Resources

- **GraphQL Documentation**: https://graphql.org/
- **Apollo Federation**: https://www.apollographql.com/docs/apollo-server/federation/introduction/
- **Ktor Documentation**: https://ktor.io/docs/
- **REST Countries API**: https://restcountries.com/
- **Java 21 LTS**: https://www.oracle.com/java/

## 📝 Commit History

Latest commits:
- ✅ `d1afd6b` - Fix: Correct devcontainer configuration for development environment
- ✅ `a30f704` - Initial commit: Federated GraphQL project with countries service restructured

## 🤝 Contributing

1. Create feature branch: `git checkout -b feature/feature-name`
2. Build and test locally: `gradle build`
3. Commit changes: `git commit -am "feat: description"`
4. Push branch: `git push origin feature/feature-name`
5. Create Pull Request on GitHub

## 📄 License

MIT

## 👤 Author

[reshavk](https://github.com/reshavk) - GitHub: https://github.com/reshavk/federated-data-graphql

---

**Last Updated**: February 22, 2026
**Project Status**: Phase 1 (Independent Services) ↪️ Phase 2 (Federation - Coming Soon)
