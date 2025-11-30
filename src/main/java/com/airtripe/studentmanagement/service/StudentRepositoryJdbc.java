// ...existing code...
package com.airtripe.studentmanagement.service;

/**
 * Compatibility wrapper so tests referencing `com.airtripe.studentmanagement.service.StudentRepositoryJdbc`
 * (no import) continue to work. It simply extends the repository implementation.
 */
public class StudentRepositoryJdbc extends com.airtripe.studentmanagement.repository.StudentRepositoryJdbc {
    // no-op; inherits all behaviour
}

