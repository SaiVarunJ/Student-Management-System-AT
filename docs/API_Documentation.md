# API Documentation

Base URL (default): http://localhost:8000

Endpoints

1) GET /students
- Description: List all students or search by query.
- Query parameters:
  - `q` (optional) - search text matched against id, name, or email.
- Response: 200 OK
- Body: JSON array of Student objects

Example request:
```sh
curl "http://localhost:8000/students"
curl "http://localhost:8000/students?q=Alice"
```

2) GET /students/{id}
- Description: Get a single student by id.
- Response codes:
  - 200 OK with student JSON
  - 404 Not Found if student doesn't exist

Example:
```sh
curl http://localhost:8000/students/S001
```

3) POST /students
- Description: Create a new student.
- Request body: JSON object with the student fields. Required fields depend on the type but generally provide `id`, `name`, `email`, `dateOfBirth` (YYYY-MM-DD). To create a graduate student include `thesisTitle` (or `thesis_title`).

Example request body (undergraduate):

```json
{
  "id": "S010",
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "dateOfBirth": "2001-07-15"
}
```

Example request body (graduate):

```json
{
  "id": "G010",
  "name": "Dr. Grad",
  "email": "grad@example.com",
  "dateOfBirth": "1995-02-20",
  "thesisTitle": "A Study on X"
}
```

- Response: 201 Created with created student JSON

Example curl:
```sh
curl -X POST -H "Content-Type: application/json" -d @student.json http://localhost:8000/students
```

4) PUT /students/{id}
- Description: Update an existing student (replace with provided data).
- Request body: same shape as POST.
- Response codes:
  - 200 OK with the updated student JSON
  - 404 Not Found if the student id does not exist

Example:

```sh
curl -X PUT -H "Content-Type: application/json" -d @updated.json http://localhost:8000/students/S001
```

5) DELETE /students/{id}
- Description: Remove a student.
- Response codes:
  - 204 No Content if removed
  - 404 Not Found if not present

Example:

```sh
curl -X DELETE http://localhost:8000/students/S001 -v
```

6) GET /metrics
- Description: Simple metrics endpoint
- Response: 200 OK
- Body: JSON object, for example:

```json
{ "studentCount": 3 }
```

7) GET /h2-console
- Description: A convenience redirect endpoint that issues a 302 redirect to the H2 web console URL. By default the app starts the H2 web console on `http://localhost:8082/` and `/h2-console` redirects there.

Notes
- Content-Type: All JSON responses are served with `Content-Type: application/json`.
- Date format: `dateOfBirth` uses ISO date `YYYY-MM-DD` and maps to Java LocalDate.
- For creating/updating graduate students the server accepts either `thesisTitle` (camelCase) or `thesis_title` (snake_case).

Error handling
- 404 Not Found for missing resources (GET/PUT/DELETE on unknown id).
- 405 Method Not Allowed for unsupported HTTP methods on endpoints.
- 500 Internal Server Error for unexpected server errors; body will include `{"error":"message"}` when available.

JSON Student shape (examples)
- Undergraduate student example:

```json
{
  "id": "S001",
  "name": "Alice",
  "email": "alice@example.com",
  "dateOfBirth": "2002-05-01"
}
```

- Graduate student example:

```json
{
  "id": "G001",
  "name": "Carol",
  "email": "carol@example.com",
  "dateOfBirth": "1995-04-10",
  "thesisTitle": "Quantum Computing Thesis"
}
```

That's the complete reference for the application's HTTP API. If you'd like, I can also add an OpenAPI/Swagger spec generated from these endpoints, or a Postman collection with pre-built requests.

This document explains how to build and run the Student Management System project locally (Windows / cmd.exe), how to configure common options, and where runtime data is stored.

Prerequisites
- Java 21 (JDK 21) on PATH. Verify with `java -version`.
- Apache Maven 3.8+ on PATH. Verify with `mvn -v`.

Build
1. Open a Windows command prompt in the project root (where `pom.xml` lives).
2. Build the project with:

```sh
mvn clean package
```

Run (development)
- Recommended (uses the exec-maven-plugin configured in the POM):

```sh
mvn exec:java
```

When you run the app you'll see console UI text and the REST server will be started automatically. By default:
- REST server listens on port 8000 and exposes `/students` and `/metrics` endpoints.
- H2 web console (optional) is started at `http://localhost:8082/` (Main starts an H2 web server).
- The default JDBC URL is an in-memory H2 DB: `jdbc:h2:mem:sms;DB_CLOSE_DELAY=-1`.
- Student data is persisted on exit to `target/students.json` by default.

Passing configuration / system properties
The application reads configuration from JVM system properties (or environment variables) via `ConfigSingleton`:
- `sms.jdbc.url` (env `SMS_JDBC_URL`) — default: `jdbc:h2:mem:sms;DB_CLOSE_DELAY=-1`
- `sms.jdbc.user` (env `SMS_JDBC_USER`) — default: `sa`
- `sms.jdbc.password` (env `SMS_JDBC_PASSWORD`) — default: empty
- `sms.data.file` (env `SMS_DATA_FILE`) — default: `target/students.json`
- `sms.h2.console.url` — used by the `/h2-console` redirect; default: `http://localhost:8082/`

To set JVM system properties when running with Maven's `exec:java`, provide them as Maven properties on the command line (Maven will forward properties to the JVM launched by the plugin):

```sh
mvn -Dsms.data.file=target/mystudents.json -Dsms.jdbc.url="jdbc:h2:~/smsdb" exec:java
```

(If you prefer, you can run the app directly from the compiled classes:)

```sh
rem Build first
mvn clean package

rem Run with JVM system properties where -D... are JVM system properties
java -Dsms.data.file=target/mystudents.json -Dsms.jdbc.url="jdbc:h2:~/smsdb" -cp target/classes;target/dependency/* com.airtripe.studentmanagement.Main
```

Notes about data persistence
- On startup the app attempts to load students from the `sms.data.file` path. Default: `target/students.json`.
- On normal shutdown the app saves all students back to that file.
- The default database is an in-memory H2 database used for runtime storage; the H2 web console is started so you can inspect the DB while the app is running.

Troubleshooting
- If the H2 console fails to start on port 8082 (port in use), Main will log a warning and continue; use `sms.h2.console.url` to override redirect target if you run H2 elsewhere.
- If you want a persistent database instead of in-memory, set `sms.jdbc.url` to a file-based H2 URL (for example `jdbc:h2:~/smsdb`) and set `sms.data.file` to a stable path.

Development tips
- Run unit tests with:

```sh
mvn test
```

- The project uses Java 21; ensure your IDE / build tool targets the same Java version.

