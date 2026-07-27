# Criteria Filter Demo

Demo application showcasing all features of the `criteria-filter` library.

## Quick Start

1. **Install criteria-filter to local Maven repository:**
   ```bash
   cd ../criteria-filter
   mvn install -DskipTests
   ```

2. **Start PostgreSQL with Docker:**
   ```bash
   docker-compose up -d
   ```

3. **Run the demo application:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access Swagger UI:**
   Open http://localhost:8080/swagger-ui.html

## Features Demonstrated

### 1. AbstractFilterController (Products)
Classic inheritance-based approach for exposing filter endpoints.

**Endpoints:**
- `POST /api/products/search` - Search products with filters
- `GET /api/products/search/schema` - Get available filter fields

### 2. @EnableFilterEndpoint (Brands)
Annotation-based automatic endpoint registration at class level.

```java
@RestController
@RequestMapping("/api/brands")
@EnableFilterEndpoint(entity = Brand.class)
public class BrandController {
    // No code needed! Endpoints are auto-registered
}
```

**Endpoints:**
- `POST /api/brands/search` - Search brands with filters
- `GET /api/brands/search/schema` - Get available filter fields

### 3. @FilterSearch/@FilterSchema (Orders)
Method-level annotations for maximum flexibility.

```java
@FilterSearch(entity = Order.class)
@PostMapping("/filter")
public FilterResult<Order> searchOrders(@RequestBody FilterRequest request, ...) {
    return null; // Method body ignored, AOP handles the logic
}
```

**Endpoints:**
- `POST /api/orders/filter` - Search orders with filters
- `GET /api/orders/filter/schema` - Get available filter fields

### 4. Filter Interceptors

#### AuditLogInterceptor
Logs all filter queries for auditing purposes. Applies to ALL entities.

#### ActiveOnlyInterceptor
Automatically adds `active = true` filter to all Product queries.
Demonstrates soft-delete or active-only filtering pattern.

## Example Queries

### Simple equality filter
```json
POST /api/products/search
{
  "filter": {
    "field": "category",
    "operator": "EQ",
    "value": "Electronics"
  }
}
```

### AND conditions
```json
POST /api/products/search
{
  "filter": {
    "and": [
      {"field": "category", "operator": "EQ", "value": "Electronics"},
      {"field": "price", "operator": "LT", "value": 1000}
    ]
  }
}
```

### OR conditions
```json
POST /api/brands/search
{
  "filter": {
    "or": [
      {"field": "country", "operator": "EQ", "value": "USA"},
      {"field": "country", "operator": "EQ", "value": "Japan"}
    ]
  }
}
```

### Nested AND/OR
```json
POST /api/orders/filter
{
  "filter": {
    "and": [
      {"field": "status", "operator": "NE", "value": "CANCELLED"},
      {
        "or": [
          {"field": "totalAmount", "operator": "GT", "value": 1000},
          {"field": "itemCount", "operator": "GTE", "value": 3}
        ]
      }
    ]
  }
}
```

### BETWEEN operator
```json
POST /api/products/search
{
  "filter": {
    "field": "price",
    "operator": "BETWEEN",
    "values": [100, 500]
  }
}
```

### IN operator
```json
POST /api/orders/filter
{
  "filter": {
    "field": "status",
    "operator": "IN",
    "values": ["PENDING", "CONFIRMED", "SHIPPED"]
  }
}
```

### LIKE operator (contains)
```json
POST /api/products/search
{
  "filter": {
    "field": "name",
    "operator": "LIKE",
    "value": "iPhone"
  }
}
```

### Nested field (brand.name)
```json
POST /api/products/search
{
  "filter": {
    "field": "brand.name",
    "operator": "EQ",
    "value": "Apple"
  }
}
```

## Available Operators

| Operator | Arity | Description |
|----------|-------|-------------|
| `EQ` | single | Equals |
| `NE` | single | Not equals |
| `GT` | single | Greater than |
| `GTE` | single | Greater or equal |
| `LT` | single | Less than |
| `LTE` | single | Less or equal |
| `LIKE` | single | Contains (case-insensitive) |
| `IN` | multi | Value in list |
| `BETWEEN` | pair | Between two values |
| `IS_NULL` | none | Is null |
| `IS_NOT_NULL` | none | Is not null |

## JSONB Filter Examples (PostgreSQL)

The `Customer` entity has JSONB fields (`preferences`, `metadata`, `address`) for testing PostgreSQL JSON operators.

### JSON_EXISTS - Check if a key exists in JSON
```json
POST /api/customers/search
{
  "filter": {
    "field": "preferences",
    "operator": "JSON_EXISTS",
    "value": "theme"
  }
}
```

### JSON_PATH_EQ - Check value at a JSON path
```json
POST /api/customers/search
{
  "filter": {
    "field": "preferences",
    "operator": "JSON_PATH_EQ",
    "values": ["theme", "dark"]
  }
}
```

### JSON_CONTAINS - Check if JSON contains a structure
```json
POST /api/customers/search
{
  "filter": {
    "field": "metadata",
    "operator": "JSON_CONTAINS",
    "value": "{\"source\": \"web\"}"
  }
}
```

### JSON_ARRAY_CONTAINS - Check if JSON array contains a value
```json
POST /api/customers/search
{
  "filter": {
    "field": "metadata",
    "operator": "JSON_ARRAY_CONTAINS",
    "values": ["tags", "vip"]
  }
}
```

### Combined: Find VIP customers with dark theme
```json
POST /api/customers/search
{
  "filter": {
    "and": [
      {"field": "preferences", "operator": "JSON_PATH_EQ", "values": ["theme", "dark"]},
      {"field": "metadata", "operator": "JSON_ARRAY_CONTAINS", "values": ["tags", "vip"]}
    ]
  }
}
```

## Sample Data

The application initializes with sample data:
- **6 Brands**: Apple, Samsung, Sony, Nike, Adidas, OldBrand (inactive)
- **17 Products**: Various electronics, footwear, and apparel
- **10 Orders**: Different statuses and amounts
- **6 Customers**: With JSONB preferences, metadata, and address fields
