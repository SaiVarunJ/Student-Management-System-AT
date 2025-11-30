// ...existing code...
package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.entity.Enrollment;
import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.repository.EnrollmentRepositoryJdbc;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EnrollmentRepositoryJdbcTest {
    private EnrollmentRepositoryJdbc repo;

    @BeforeEach
    void setup() throws Exception {
        repo = new EnrollmentRepositoryJdbc();
        repo.init();
        try (Connection c = ConfigSingleton.getInstance().getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM enrollments");
            st.executeUpdate("DELETE FROM students");
            st.executeUpdate("DELETE FROM courses");
        }
    }

    @Test
    void addFindUpdateDeleteAndQueries() {
        Student s = StudentFactory.createUndergraduate("S10", "Stu", "stu@example.com", LocalDate.of(1999,1,1));
        Course c = new Course("C10", "Course 10", 5);
        Enrollment e = new Enrollment(s, c, LocalDate.now());
        repo.add(e);

        Optional<Enrollment> found = repo.find(s, c);
        assertTrue(found.isPresent());

        List<Enrollment> byStudent = repo.findByStudentId(s.getId());
        assertEquals(1, byStudent.size());

        List<Enrollment> byCourse = repo.findByCourseId(c.getId());
        assertEquals(1, byCourse.size());

        // update grade
        e.setGrade(7.5);
        repo.add(e);
        Optional<Enrollment> found2 = repo.find(s, c);
        assertTrue(found2.isPresent());
        assertTrue(found2.get().getGrade().isPresent());
        assertEquals(7.5, found2.get().getGrade().get(), 0.0001);

        // delete
        assertTrue(repo.remove(s.getId(), c.getId()));
        assertFalse(repo.find(s, c).isPresent());
    }
}

