# Federation Design & Phase 2 Migration Strategy

This document outlines how Phase 1 (Countries Service) is designed to support seamless federation in Phase 2, where additional services will reference and extend country data.

## Phase 1: Foundation

### Primary Entities

The Countries Service exposes three federation-ready entities:

#### 1. **Country** (Primary)

```graphql
type Country @key(fields: "cca2") {
  cca2: String! # ISO 3166-1 alpha-2 code (immutable, globally unique)
  cca3: String! # ISO 3166-1 alpha-3 code
  name: String! # Common name
  officialName: String # Official/formal name
  region: String! # Geographic region
  subregion: String # Sub-region classification
  capital: [String!]! # Capital city names
  languages: [Language!]! # Official languages
  currencies: [Currency!]! # Official currencies
  borders: [Country!]! # Neighboring countries
  continents: [String!]! # Continents
  coordinates: Coordinates # Geographic coordinates
  area: Float # Land area in km²
  population: Int # Population count
  timezones: [String!]! # Time zones
  flag: String # Flag emoji
}
```

**Design Decisions:**

- `cca2` is the `@key` field because:
  - ✅ Globally unique and standardized (ISO 3166-1)
  - ✅ Never changes (immutable)
  - ✅ Universally recognized in other systems
  - ✅ Compact (2 characters)

#### 2. **Language** (Shareable Reference Type)

```graphql
type Language @shareable {
  code: String! # ISO 639-1 language code (immutable)
  name: String! # English language name
}
```

**Design Decisions:**

- Marked `@shareable` because Phase 2 services may need to extend with:
  - Native speakers count
  - Language families / language trees
  - Multilingual content metadata
  - Language proficiency data

#### 3. **Currency** (Shareable Reference Type)

```graphql
type Currency @shareable {
  code: String! # ISO 4217 currency code (immutable)
  symbol: String # Currency symbol
  name: String! # Currency name
}
```

**Design Decisions:**

- Marked `@shareable` because Phase 2 services may extend with:
  - Exchange rates
  - Historical rates
  - Inflation data
  - Payment method support

### Data Consistency Rules

| Rule                                                        | Purpose                                       |
| ----------------------------------------------------------- | --------------------------------------------- |
| `cca2`, `cca3`, language codes, currency codes never change | Ensures federation references remain valid    |
| `borders: [Country!]!` returns actual Country objects       | Allows Phase 2 to query neighboring countries |
| Language/Currency arrays never null (minimum empty array)   | Prevents null-handling complexity in Phase 2  |
| `name` field always present and non-null                    | Guaranteed for Phase 2 reference resolution   |

## Phase 2: Federation Composition

### Scenario: Travel Service

When Phase 2 builds a Travel/Tourism service, it will reference Phase 1 entities:

```graphql
# Phase 2: Travel Service Schema

type Query {
  travelPackages(destination: String, currency: String): [TravelPackage!]!
  travelPackage(id: ID!): TravelPackage
}

type TravelPackage @key(fields: "id") {
  id: ID!
  title: String!
  description: String!

  # References to Phase 1 Countries Service
  destination: Country!
  includedCountries: [Country!]!
  excludedCountries: [Country!]

  # References to Phase 1 Currency Service
  baseCurrency: Currency!
  pricingByCountry: [PricingByCountry!]!

  # References to Phase 1 Language Service
  availableLanguages: [Language!]!
  defaultLanguage: Language!

  # Travel-specific data
  startDate: String!
  endDate: String!
  basePrice: Float!
  duration: Int! # days
  maxParticipants: Int
}

type PricingByCountry {
  country: Country!
  localCurrency: Currency!
  localPrice: Float!
}

# Extend Phase 1's Country to show travel packages
extend type Country {
  availableTravelPackages: [TravelPackage!]!
}

# Extend Phase 1's Currency to show pricing
extend type Currency {
  packagesUsingCurrency: [TravelPackage!]!
}
```

### Federation Queries

With Apollo Gateway, clients can make unified queries like:

```graphql
query {
  travelPackage(id: "pkg-123") {
    title
    destination {
      name
      capital
      flag
      languages {
        name
      }
      currencies {
        code
        symbol
        name
      }
    }
    baseCurrency {
      code
      symbol
      name
    }
    availableLanguages {
      code
      name
    }
  }
}
```

**Behind the scenes:**

- Apollo Gateway routes `destination` request to Phase 1 (Countries Service)
- Apollo Gateway routes `baseCurrency` request to Phase 1 (Countries Service)
- Apollo Gateway routes `availableLanguages` request to Phase 1 (Countries Service)
- All results are composed into single response

### Alternative Phase 2 Service: eCommerce

Another Phase 2 service example (Products/eCommerce):

```graphql
type Product @key(fields: "sku") {
  sku: String!
  name: String!
  basePrice: Float!

  # References Phase 1
  manufacturer: Country!
  shippingCountries: [Country!]!
  baseCurrency: Currency!

  # Multi-language content
  descriptions: [LocalizedContent!]!
}

type LocalizedContent {
  language: Language!
  title: String!
  description: String!
}

extend type Country {
  manufacturingHubs: [Product!]!
  manufacturedProducts(category: String): [Product!]!
}
```

## Implementation: Phase 2 Service

### Step 1: Apollo Gateway Setup

Deploy Apollo Gateway (separate service or in existing cluster) that:

- Discovers Phase 1 and Phase 2 subgraph services
- Composes federated schema on startup
- Routes queries to appropriate services
- Merges results using `__typename` and `@key` fields

```graphql
# Apollo Gateway sees composed schema like:

type Country @key(fields: "cca2") {
  cca2: String!
  name: String!
  # ... Phase 1 fields

  # Extended by Phase 2 Travel Service
  availableTravelPackages: [TravelPackage!]!

  # Extended by Phase 2 eCommerce Service
  manufacturingHubs: [Product!]!
}

type TravelPackage @key(fields: "id") {
  id: ID!
  destination: Country!
  # ... travel-specific fields
}

type Product @key(fields: "sku") {
  sku: String!
  manufacturer: Country!
  # ... product-specific fields
}
```

### Step 2: Entity Resolution

When Phase 2 service needs to resolve a Country:

```kotlin
// Phase 2 Travel Service

@DgsDataFetcher
fun countryReference(
  @DgsArgument _representations: List<Map<String, Any>>
): List<Country?> {
  return _representations.map { ref ->
    val cca2 = ref["cca2"] as String
    // Call Phase 1 Countries Service to fetch full Country
    countriesServiceClient.getCountry(cca2)
  }
}
```

### Step 3: Reverse Extensions

Phase 1 can extend with reverse references:

```graphql
# Phase 1 extended with Phase 2 data

extend type Country {
  travelPackages: [TravelPackage!]!
  products: [Product!]!
}
```

Implementation:

```kotlin
// Phase 1: Add resolver for travelPackages extension

@DgsDataFetcher
fun travelPackages(env: DataFetchingEnvironment): List<TravelPackage> {
  val country = env.getSource<Country>()

  // Call Phase 2 Travel Service
  return travelServiceClient.getPackagesByDestination(country.cca2)
}
```

## API Contract for Phase 2 Services

### Guarantees Phase 1 Provides

1. **Stable Identifiers**: `cca2` never changes
2. **Required Fields**: `cca2`, `name`, `region`, `continents` are always present
3. **Immutable Core Data**: Language codes, currency codes don't change
4. **Reference Integrity**: Borders always refer to valid country codes
5. **Uptime SLA**: Phase 1 should maintain 99.5%+ availability
6. **Cache-Friendly**: Caching with 1-hour TTL is acceptable

### Phase 2 Service Responsibilities

1. **Entity References**: Only reference by `cca2` code
2. **Error Handling**: Handle Phase 1 timeouts gracefully
3. **Batch Operations**: Use batch loaders for efficiency (avoid N+1)
4. **Consistency**: Don't duplicate Country data; always reference

## ECS Deployment Architecture

### Single Cluster, Multiple Services

```
┌─────────────────────────────────────────────────────┐
│         AWS ECS Cluster                             │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │ Countries Service│  │  Travel Service  │        │
│  │   (Phase 1)      │  │   (Phase 2a)     │        │
│  └──────────────────┘  └──────────────────┘        │
│                                                     │
│  ┌──────────────────┐  ┌──────────────────┐        │
│  │ eCommerce Service│  │ Apollo Gateway   │        │
│  │   (Phase 2b)     │  │ (Composition)    │        │
│  └──────────────────┘  └──────────────────┘        │
│                                                     │
│  Service Discovery: ECS Service Connect             │
│  Load Balancer: ALB → Apollo Gateway (8080)        │
│                                                     │
└─────────────────────────────────────────────────────┘
        ↓
    Clients
```

### Environment Configuration (Phase 2)

```env
# Phase 2 Travel Service
COUNTRIES_SERVICE_URL=http://countries-service:8080/graphql
COUNTRIES_SERVICE_TIMEOUT=5000

# Apollo Gateway
SUBGRAPH_COUNTRIES_URL=http://countries-service:8080/graphql
SUBGRAPH_TRAVEL_URL=http://travel-service:8080/graphql
SUBGRAPH_ECOMMERCE_URL=http://ecommerce-service:8080/graphql
```

## Schema Evolution Rules

### What Can Change in Phase 1 (Non-Breaking)

✅ Add new fields to Country type  
✅ Add new query types  
✅ Add new `@shareable` types  
✅ Extend Language or Currency with new fields

### What Cannot Change in Phase 1 (Breaking)

❌ Remove or rename `cca2`, `name` fields  
❌ Change `cca2` from `@key` field  
❌ Remove Language or Currency fields  
❌ Make required fields nullable  
❌ Change field types

## Phase 2 Migration Checklist

- [ ] Deploy Apollo Gateway service
- [ ] Register Phase 1 (Countries Service) as subgraph
- [ ] Build Phase 2 service with `@reference` directives for Country
- [ ] Register Phase 2 service with Apollo Gateway
- [ ] Test unified GraphQL queries across both services
- [ ] Set up ECS service discovery for inter-service communication
- [ ] Configure monitoring/alerting for federation latency
- [ ] Document Phase 3 expansion path for additional services

## Benefits of This Design

| Benefit              | How Achieved                                            |
| -------------------- | ------------------------------------------------------- |
| No data duplication  | Phase 2 references, not copies, Country data            |
| Consistent schema    | Single source of truth (Phase 1) for country info       |
| Easy to scale        | Add Phase 3, Phase 4 services without modifying Phase 1 |
| Clear boundaries     | Each service owns specific domain                       |
| Production-ready     | Designed for real-world federation patterns             |
| Type-safe references | `cca2` is immutable, indexed, efficient                 |

## Troubleshooting Federation

### Issue: "Unknown type Country in subgraph"

**Cause**: Phase 2 schema references Country without importing from Phase 1  
**Fix**: Ensure Apollo Gateway composition includes Phase 1 subgraph URL

### Issue: Circular references between services

**Cause**: Phase 1 extends with Phase 2 data, Phase 2 references Phase 1  
**Fix**: Use careful resolver implementation; fetch data on-demand only

### Issue: N+1 queries to Phase 1

**Cause**: Phase 2 resolver fetches countries one-by-one  
**Fix**: Implement batch loader in Phase 2 service

### Issue: Service timeout due to federation

**Cause**: Phase 2 references country that doesn't exist  
**Fix**: Add fallback/null handling in Phase 2 resolvers

## Next Phase: Phase 3 Analytics Service

Future services can reference both Phase 1 and Phase 2:

```graphql
type CountryAnalytics @key(fields: "countryCode") {
  countryCode: String!
  country: Country! # Reference Phase 1
  visitationStats: VisitationStats
  travelPopularity: [TravelPackage!]! # Reference Phase 2
  productSalesData: [Product!]! # Reference Phase 2
}
```

The federation model scales horizontally to N services, all coordinated through Apollo Gateway.
