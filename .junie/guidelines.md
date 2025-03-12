# Hotel API Developer Guidelines

## Project Overview
This project is a RESTful API for managing hotels and rooms, built with Java 17 and modern web technologies.

## Project Structure
- `src/main/java/dat/`
  - `config/` - Application and Hibernate configuration
  - `controllers/` - REST API controllers
  - `dao/` - Data Access Objects for database operations
  - `dto/` - Data Transfer Objects for API requests/responses
  - `entities/` - JPA entity classes
  - `exceptions/` - Custom exception classes
  - `routes/` - API route definitions
  - `utils/` - Utility classes
- `src/main/resources/` - Configuration files
- `src/test/` - Test classes and resources

## Tech Stack
- **Java 17** - Programming language
- **Maven** - Build tool and dependency management
- **Javalin** - Lightweight web framework
- **Hibernate** - ORM for database access
- **PostgreSQL** - Database
- **SLF4J/Logback** - Logging
- **Jackson** - JSON serialization/deserialization
- **Lombok** - Reduces boilerplate code

## Testing
- **JUnit 5** - Testing framework
- **REST Assured** - API testing
- **TestContainers** - Integration testing with PostgreSQL
- **Hamcrest** - Assertions

### Running Tests
```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=TestResourceTest
```

## Building and Running
```bash
# Build the project
mvn clean package

# Run the application
java -jar target/app.jar
```

## API Usage
The API provides endpoints for managing hotels and rooms:
- `GET /api/routes` - List all available routes
- `GET /api/hotel` - Get all hotels
- `GET /api/hotel/{id}` - Get a specific hotel
- `POST /api/hotel` - Create a new hotel
- `PUT /api/hotel/{id}` - Update a hotel
- `DELETE /api/hotel/{id}` - Delete a hotel
- `GET /api/hotel/{id}/rooms` - Get rooms for a hotel

Example requests can be found in `src/main/java/dat/demo.http`.

## Best Practices
1. **Error Handling** - Use try-catch blocks and proper error responses
2. **Logging** - Use SLF4J for logging errors and important information
3. **Input Validation** - Validate input parameters using Javalin's validation API
4. **DTOs** - Use DTOs to separate API representation from entity model
5. **Consistent Responses** - Use appropriate HTTP status codes and error messages
6. **Separation of Concerns** - Keep controllers, DAOs, and entities separate
7. **Testing** - Write tests for all API endpoints and database operations