# AlgoRythm backend

Spring Boot 3 (Java 21) backend for AlgoRythm. This is the scaffold — just the app and
a health endpoint. Database, auth, and features are added in later issues.

## Requirements
- JDK 21
- Maven 3.9+ (or use your IDE's bundled Maven)

## Run locally
```bash
cd backend
mvn spring-boot:run
```
The app starts on http://localhost:8080. Check it's alive:
```bash
curl http://localhost:8080/api/health
# {"status":"ok","service":"algorythm-backend","time":"..."}
```

## Build / test
```bash
mvn clean verify
```

## Layout
```
src/main/java/com/algorythm/
  AlgorythmApplication.java   # entry point
  controller/                 # REST endpoints (thin; delegate to services)
  service/                    # business logic
  repository/                 # Spring Data repositories (added with the DB)
  config/                     # CORS, security, etc. (added as needed)
  model/                      # domain entities
src/main/resources/
  application.properties      # config (port, etc.)
src/test/java/...             # tests (contextLoads smoke test)
```

All routes live under `/api`. Docker and the database come in the following issues, at
which point the backend runs as part of `docker compose up --build` alongside the
frontend.
