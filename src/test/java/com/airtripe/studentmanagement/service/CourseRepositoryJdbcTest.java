package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.repository.CourseRepositoryJdbc;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CourseRepositoryJdbcTest {
    private CourseRepositoryJdbc repo;

    @BeforeEach
    void setup() throws Exception {
        repo = new CourseRepositoryJdbc();
        repo.init();
        try (Connection c = ConfigSingleton.getInstance().getConnection(); Statement st = c.createStatement()) {
            try { st.executeUpdate("DELETE FROM enrollments"); } catch (Exception ignored) {}
            try { st.executeUpdate("DELETE FROM students"); } catch (Exception ignored) {}
            try { st.executeUpdate("DELETE FROM courses"); } catch (Exception ignored) {}
        }
    }

    @Test
    void addFindUpdateDeleteAndSearch() {
        Course c = new Course("C1", "Test Course", 3);
        repo.addCourse(c);

        Optional<Course> found = repo.findById("C1");
        assertTrue(found.isPresent());
        assertEquals("Test Course", found.get().getName());

        // update
        Course updated = new Course("C1", "Updated Course", 4);
        assertTrue(repo.updateCourse("C1", updated));
        Course found2 = repo.findById("C1").orElseThrow();
        assertEquals("Updated Course", found2.getName());

        // search
        List<Course> results = repo.search("Updated");
        assertEquals(1, results.size());

        // delete
        assertTrue(repo.remove("C1"));
        assertFalse(repo.findById("C1").isPresent());
    }
}
