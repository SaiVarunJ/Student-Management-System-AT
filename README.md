# Student Management System (AT)

A small Java project demonstrating object-oriented design and common design patterns used in a student management system assignment.

This repository contains a simple, well-tested scaffold that exercises: OOP, Streams, Optional, LocalDate, Lambdas, and design patterns such as Singleton, Factory, and Observer.

## Key features
- Basic student, course and enrollment domain model
- Persistence-backed services (testable via unit tests)
- Observer pattern usage for enrollment/grade notifications
- Examples of service and repository layering

## Requirements
- Java 11 or newer (recommended)
- Maven 3.6+

## Quick start
1. Open a command prompt in the project root (where `pom.xml` is located).
2. Run the unit tests with Maven:

    ```cmd
    mvn test
    ```

    (or to run quietly)

    ```cmd
    mvn -q test
    ```

3. Build a runnable JAR:

    ```cmd
    mvn package
    ```

4. Run the packaged JAR (artifact produced in `target/`):

    ```cmd
    java -jar target/student-management-system-1.0.0.jar
    ```

## Project structure (important folders)
- `src/main/java` - application sources (packages under `com.airtripe.studentmanagement`)
- `src/main/resources` - runtime resources (logging configuration, etc.)
- `src/test/java` - JUnit 5 unit tests
- `target/` - Maven build outputs (compiled classes, packaged JAR, surefire reports)

## Running tests and reports
- Tests use JUnit 5 and are executed by `mvn test`.
- Test reports are written to `target/surefire-reports/` (useful for CI or investigation of failures).

## Notes and tips
- The project was created as an assignment scaffold — it's intentionally small and focused on demonstrating patterns and unit testing.
- If you encounter Java compatibility errors, confirm `JAVA_HOME` points to a Java 11+ installation and Maven is using that JVM.

## Contributing
- Feel free to open issues or add small PRs to improve tests, add features, or modernize code. Keep changes focused and include/adjust unit tests.

## License
- None specified in this repository. Check with the project owner if you plan to redistribute.

## Contact
- For questions about this codebase, look at the source under `src/main/java/com/airtripe/studentmanagement` or reach out to the repository owner.

---

This README replaces the lightweight scaffold note with clearer instructions to help contributors and graders run and test the project locally.
