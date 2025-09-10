# Cross-Platform Testing Guide

This document explains how to run Artipie tests on both ARM64 and AMD64 architectures.

## Overview

The Artipie project has been configured to support cross-platform testing using Testcontainers with PostgreSQL. The tests are designed to work seamlessly on both ARM64 (Apple Silicon, ARM-based servers) and AMD64 (Intel/AMD processors) architectures.

## Architecture Support

- **AMD64**: Intel and AMD x86_64 processors
- **ARM64**: Apple Silicon (M1/M2/M3), ARM-based servers, and other ARM64 processors

## Test Configuration

### PostgreSQL Test Configuration

The project uses a centralized `PostgreSQLTestConfig` class that provides:

- Cross-platform PostgreSQL container configuration
- Consistent database settings across all tests
- Container reuse for better performance
- Proper labeling for container management

### Testcontainers Configuration

The following configuration files ensure cross-platform compatibility:

1. **`.testcontainers.properties`** (project root):
   ```properties
   testcontainers.reuse.enable=true
   testcontainers.checks.disable=true
   ```

2. **Maven Surefire Plugin** (in `pom.xml`):
   ```xml
   <systemPropertyVariables>
       <testcontainers.reuse.enable>true</testcontainers.reuse.enable>
       <testcontainers.checks.disable>true</testcontainers.checks.disable>
   </systemPropertyVariables>
   ```

## Running Tests

### Prerequisites

1. **Docker**: Ensure Docker is installed and running
2. **Java 21**: Required for compilation and testing
3. **Maven**: For building and running tests

### Basic Test Execution

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest="DbConsumerTest"

# Run specific test method
mvn test -Dtest="DbConsumerTest#addsAndRemovesRecord"
```

### Architecture-Specific Testing

#### On AMD64 Systems
```bash
# Tests will automatically use AMD64-compatible PostgreSQL images
mvn clean test
```

#### On ARM64 Systems
```bash
# Tests will automatically use ARM64-compatible PostgreSQL images
mvn clean test
```

### Integration Tests

For integration tests that require external PostgreSQL connections, you can configure connection parameters:

```bash
# Set custom PostgreSQL connection parameters
mvn test -Dtest.postgres.host=localhost \
         -Dtest.postgres.port=5432 \
         -Dtest.postgres.database=artifacts \
         -Dtest.postgres.user=artipie \
         -Dtest.postgres.password=artipie
```

## Test Classes

The following test classes have been updated for cross-platform compatibility:

- `DbConsumerTest` - Database consumer functionality tests
- `ArtifactDbTest` - Database factory and connection tests  
- `SchedulerDbTest` - Scheduler with database integration tests
- `MetadataMavenITCase` - Maven integration tests (configurable)

## Docker Images

The tests use the `postgres:15-alpine` Docker image, which:

- Supports both ARM64 and AMD64 architectures
- Is lightweight and fast to start
- Provides consistent PostgreSQL behavior across platforms

## Troubleshooting

### Container Reuse Issues

If you see warnings about container reuse not being enabled:

1. Ensure `.testcontainers.properties` exists in the project root
2. Check that the file contains the correct configuration
3. Restart your IDE/terminal session

### Architecture Detection Issues

If tests fail to start PostgreSQL containers:

1. Verify Docker is running: `docker --version`
2. Check Docker architecture support: `docker info | grep Architecture`
3. Ensure you have the latest Docker version with multi-arch support

### Performance Issues

For better performance on resource-constrained systems:

1. Enable container reuse (already configured)
2. Use `mvn test -Dtestcontainers.reuse.enable=true`
3. Consider running tests in smaller batches

## CI/CD Considerations

For continuous integration on multiple architectures:

1. **GitHub Actions**: Use matrix builds with different runner types
2. **Docker Buildx**: Enable multi-platform builds
3. **Testcontainers**: Ensure CI environment supports Docker-in-Docker

Example GitHub Actions matrix:
```yaml
strategy:
  matrix:
    os: [ubuntu-latest, macos-latest]
    architecture: [amd64, arm64]
```

## Migration Notes

The following changes were made to support cross-platform testing:

1. **Centralized Configuration**: Created `PostgreSQLTestConfig` class
2. **Container Reuse**: Enabled for better performance
3. **Flexible Integration Tests**: Made connection parameters configurable
4. **Maven Configuration**: Added Testcontainers system properties
5. **Documentation**: Added comprehensive testing guide

## Support

For issues related to cross-platform testing:

1. Check this documentation first
2. Review Testcontainers documentation: https://www.testcontainers.org/
3. Check Docker multi-architecture support: https://docs.docker.com/buildx/working-with-buildx/
4. Open an issue in the Artipie repository with architecture details
