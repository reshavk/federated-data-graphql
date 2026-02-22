# Local Development Setup

## Option 1: Using Devcontainer (Recommended)

This project includes a devcontainer configuration that provides a consistent Java 21 LTS development environment with Gradle pre-configured.

### Requirements

- Visual Studio Code
- Docker Desktop
- Remote - Containers extension for VS Code

### Setup

1. Clone the repository
2. Open in VS Code
3. Click the "Reopen in Container" button when prompted, or:
   - Press `Cmd/Ctrl + Shift + P`
   - Search for "Reopen in Container"
   - Press Enter

VS Code will automatically:

- Build the devcontainer image
- Mount your workspace
- Install necessary extensions
- Configure the environment

### Using the Devcontainer

Once inside the devcontainer, you can run commands directly in the VS Code terminal:

```bash
# Build the project
gradle build

# Run tests
gradle test

# Run the service locally
gradle run

# Build fat JAR
gradle fatJar

# Check code
gradle check
```

## Option 2: Local Development (Java 21 Required)

If you prefer not to use devcontainers:

### Prerequisites

- Java 21 LTS (or newer)
- Gradle 8.4+ (or use the wrapper)
- Git

### Verify Setup

```bash
java -version
gradle --version
```

Output should show:

- Java 21.x.x
- Gradle 8.4+

### Build and Run

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run the service
./gradlew run

# The service will start on http://localhost:8080
```

## Testing the Service

### Health Check

```bash
curl http://localhost:8080/health
# Expected: {"status":"UP"}
```

### Sample GraphQL Query

```bash
curl -X POST http://localhost:8080/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ country(cca2: \"US\") { cca2 name region population } }"
  }'
```

### Interactive GraphQL Testing

With the service running, you can test queries in your browser or with tools like:

- [GraphQL Playground](https://www.apollographql.com/docs/apollo-server/testing/graphql-playground/)
- [Insomnia](https://insomnia.rest/)
- [VS Code REST Client extension](https://marketplace.visualstudio.com/items?itemName=humao.rest-client)

## Environment Variables

Create a `.env` file in the project root (optional):

```env
PORT=8080
REST_COUNTRIES_URL=https://restcountries.com/v3.1
```

The service will use defaults if not specified.

## Gradle Tasks

Common Gradle commands:

```bash
# View available tasks
gradle tasks

# Build
gradle build                    # Full build with tests
gradle build -x test          # Build without running tests
gradle classes                # Compile only
gradle fatJar                 # Build executable JAR

# Running
gradle run                    # Run the application

# Testing
gradle test                   # Run all tests
gradle test --tests "*ClientTest"  # Run specific test

# Cleanup
gradle clean                  # Remove build artifacts
gradle cleanBuild            # Clean then build

# Code Quality
gradle check                 # Run all checks (tests, lint, etc.)
gradle build --scan         # Build with Gradle Build Scan
```

## Troubleshooting

### "Gradle daemon is not running"

```bash
gradle --stop
gradle build
```

### "Java version incompatibility"

Verify you're using Java 21:

```bash
java -version  # Should show 21.x.x
```

### "Port 8080 already in use"

Change the port:

```bash
PORT=9000 gradle run
```

### "REST Countries API timeout"

The service calls https://restcountries.com/v3.1/ - ensure you have internet access. You can mock it in tests.

## IDE Setup

### IntelliJ IDEA / Android Studio

1. Open project
2. Choose "Open as Gradle project" if prompted
3. Go to Settings → Languages & Frameworks → Kotlin
4. Choose Gradle as the compiler

### VS Code (without Devcontainer)

1. Install Kotlin Language extension (fwcd.kotlin)
2. Install Java Extension Pack (Microsoft)
3. Open project folder
4. VS Code will detect Gradle project

## Next Steps

- See [README.md](../README.md) for API documentation
- See [FEDERATION_DESIGN.md](../FEDERATION_DESIGN.md) for Phase 2 roadmap
- Check [src/test/kotlin](../src/test/kotlin) for example tests
