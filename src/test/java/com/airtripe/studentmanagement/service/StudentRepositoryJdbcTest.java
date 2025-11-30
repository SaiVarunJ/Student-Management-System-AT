package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StudentRepositoryJdbcTest {
    private StudentRepositoryJdbc repo;

    @BeforeEach
    void setup() throws Exception {
        repo = new StudentRepositoryJdbc();
        repo.init();
        // clear table for test isolation
        try (Connection c = ConfigSingleton.getInstance().getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM students");
        }
    }

    @Test
    void addFindUpdateDeleteAndSearch() {
        Student s = StudentFactory.createUndergraduate("T1", "Test", "t@example.com", LocalDate.of(2000,1,1));
        repo.addStudent(s);

        Optional<Student> found = repo.findById("T1");
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getName());

        // update
        Student updated = StudentFactory.createUndergraduate("T1", "Updated", "u@example.com", LocalDate.of(2001,2,2));
        assertTrue(repo.updateStudent("T1", updated));
        Student found2 = repo.findById("T1").orElseThrow();
        assertEquals("Updated", found2.getName());

        // search
        List<Student> results = repo.search("Updated");
        assertEquals(1, results.size());

        // delete
        assertTrue(repo.remove("T1"));
        assertFalse(repo.findById("T1").isPresent());

        // previously we exposed metrics; metrics removed so nothing to assert here
    }
}
