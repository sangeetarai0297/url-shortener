# url-shortener
URL Shortener - Production-Ready Spring Boot Application
A complete, production-ready URL shortener system built with Spring Boot, PostgreSQL, and Redis. This application provides RESTful APIs to shorten URLs and redirect to the original URLs with caching support.

Features
✅ URL Shortening: Convert long URLs into short, memorable codes
✅ Deduplication: Same URL returns the same short code
✅ Collision Handling: Sophisticated algorithm with fallback mechanisms
✅ Caching: Redis-backed caching for fast redirects
✅ Database: PostgreSQL for persistent storage
✅ Validation: Input validation with error handling
✅ Logging: Comprehensive logging for debugging
✅ Exception Handling: Global exception handler with meaningful error responses
✅ Layered Architecture: Clean separation of concerns
✅ REST API: Well-documented endpoints

Tech Stack
Java 21 - Latest LTS version
Spring Boot 4.0.4 - Latest Spring Boot
Spring Data JPA - ORM
PostgreSQL - Primary Database
Redis - Caching Layer
Lombok - Boilerplate reduction
Jakarta Validation - Input validation
Architecture
com.example.urlshortener/
├── controller/          # REST endpoints
├── service/             # Business logic
├── repository/          # Data access layer
├── model/               # Entity and DTOs
├── utility/             # Helper classes (HashUtil)
├── exception/           # Custom exceptions and handlers
├── config/              # Spring configuration
└── UrlshortenerApplication.java
Prerequisites
Java 21 - Download from oracle.com
PostgreSQL 12+ - Download from postgresql.org
Redis - Download from redis.io or use Docker:
docker run -d -p 6379:6379 redis:latest
Gradle - Included via Gradle Wrapper
Installation & Setup
1. Create PostgreSQL Database
CREATE DATABASE urlshortener_db;
2. Update Configuration
Edit src/main/resources/application.properties:

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/urlshortener_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Application
app.short-url.base-url=http://localhost:8080
3. Build the Project
cd urlshortener
./gradlew clean build
4. Run the Application
./gradlew bootRun
The application will start on http://localhost:8080

API Endpoints
1. Shorten URL
Endpoint: POST /shorten

Request:

{
  "longUrl": "https://www.example.com/very/long/url/path"
}
Response (201 Created):

{
  "shortUrl": "http://localhost:8080/abc1234",
  "longUrl": "https://www.example.com/very/long/url/path",
  "shortCode": "abc1234"
}
Example with cURL:

curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"longUrl":"https://www.example.com"}'
2. Redirect to Original URL
Endpoint: GET /{shortCode}

Response: HTTP 302 Redirect to the original URL

Example with cURL:

curl -i http://localhost:8080/abc1234
Output:

HTTP/1.1 302 Found
Location: https://www.example.com/very/long/url/path
3. Health Check
Endpoint: GET /health

Response (200 OK):

URL Shortener service is running
Algorithm Details
URL Shortening Process
Input Validation

Check URL format (must start with http:// or https://)
Deduplication

Query database for existing URL
If found, return existing short code
Hash Generation

Use SHA-1 hashing algorithm
Extract first 7 characters as short code
Collision Handling

Check if short code already exists
If collision detected:
Try alternative codes (shift substring)
Support up to 5 attempts
If still collisions, use timestamp-based rehashing
Storage

Save to PostgreSQL database
Cache in Redis (24-hour TTL)
Response

Return complete short URL with base URL
Caching Strategy
Cache Key: short_url:{shortCode}
Cache Value: Original long URL
TTL: 24 hours
Cache-First Approach:
Check Redis cache
If miss, query PostgreSQL
Store result in cache for future requests
Database Schema
URLs Table
CREATE TABLE urls (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  long_url VARCHAR(2048) NOT NULL,
  short_code VARCHAR(7) NOT NULL UNIQUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_short_code (short_code)
);
Error Handling
The application uses a global exception handler with meaningful error responses:

Invalid URL (400 Bad Request)
{
  "timestamp": "2026-03-20T10:30:45",
  "status": 400,
  "error": "Invalid URL",
  "message": "Invalid URL format. URL must start with http:// or https://",
  "path": "uri=/shorten"
}
URL Not Found (404 Not Found)
{
  "timestamp": "2026-03-20T10:30:45",
  "status": 404,
  "error": "URL not found",
  "message": "Short code not found: nonexistent",
  "path": "uri=/nonexistent"
}
Server Error (500 Internal Server Error)
{
  "timestamp": "2026-03-20T10:30:45",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "path": "uri=/shorten"
}
Logging Configuration
Logging is configured in application.properties:

logging.level.root=INFO
logging.level.com.example.urlshortener=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
Log Examples:

URL creation: Successfully shortened URL. ShortCode: abc1234, LongUrl: https://...
Redirect: Retrieved original URL for shortCode: abc1234
Errors: Invalid URL format: invalid-url
Testing
Run Unit Tests
./gradlew test

Manual Testing with Postman
Create Short URL

Method: POST
URL: http://localhost:8080/shorten
Body: {"longUrl":"https://www.example.com"}
Redirect

Method: GET
URL: http://localhost:8080/{shortCode}
Best Practices Implemented
✅ Clean Code

Clear naming conventions
Single responsibility principle
DRY (Don't Repeat Yourself)
✅ Spring Boot Best Practices

Constructor injection (not field injection)
@Component/@Service/@Repository annotations
Proper use of Optional
@Transactional for database operations
✅ Exception Handling

Custom exceptions
Global exception handler
Meaningful error messages
✅ Logging

Appropriate log levels
Sensitive information excluded
Performance tracking
✅ Database

Unique constraint on short_code
Index on short_code for fast lookups
Proper data types and constraints
✅ Caching

Redis configuration with connection pooling
Appropriate TTL settings
Graceful fallback on cache miss
Performance Considerations
Database Indexing: Short code has index for O(1) lookup
Caching: Most frequent lookups served from Redis (~1ms vs ~10-20ms from DB)
Connection Pooling: Redis and PostgreSQL use connection pools
Lazy Initialization: Beans are loaded on demand
Collision Handling: Efficient algorithm with early termination
Production Deployment Checklist
Update PostgreSQL credentials in application.properties
Update Redis host/port for production environment
Update app.short-url.base-url to production domain
Set spring.jpa.hibernate.ddl-auto=validate (don't auto-create tables)
Configure SSL/HTTPS
Set up proper logging (external log aggregation)
Configure database backups
Set up Redis persistence
Use environment variables for sensitive configuration
Enable Spring Security if needed
Configure rate limiting
Set up monitoring and alerting
Troubleshooting
PostgreSQL Connection Error
Could not connect to 'localhost:5432'
Solution: Ensure PostgreSQL is running and database exists

psql -U postgres -c "CREATE DATABASE urlshortener_db;"
Redis Connection Error
Unable to connect to Redis at localhost:6379
Solution: Ensure Redis is running

redis-cli ping  # Should return PONG
Application Startup Error
Parameter 0 of constructor in UrlService required a bean
Solution: Ensure all dependencies are in classpath and beans are properly configured

Future Enhancements
Analytics (track number of redirects per URL)
Custom short codes (user-provided)
URL expiration (TTL for short codes)
User authentication and URL management
URL preview feature
QR code generation
Rate limiting by IP
Database replication for HA
Redis cluster setup
License
MIT License - Feel free to use this project for commercial purposes.

Support
For issues and questions, please create an issue in the repository.

Built with ❤️ using Spring Boot
