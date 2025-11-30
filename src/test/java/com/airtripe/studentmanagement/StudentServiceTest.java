package com.airtripe.studentmanagement;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.service.StudentService;
import com.airtripe.studentmanagement.factory.StudentFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class StudentServiceTest {
    @Test
    void addAndFindStudent() {
        StudentService ss = new StudentService();
        Student s = StudentFactory.createUndergraduate("U1","Test","t@example.com", LocalDate.now());
        ss.addStudent(s);
        assertTrue(ss.findById("U1").isPresent());
    }

    @Test
    void searchReturnsMatches() {
        StudentService ss = new StudentService();
        Student s1 = StudentFactory.createUndergraduate("U1","Alice","a@example.com", LocalDate.now());
        Student s2 = StudentFactory.createUndergraduate("U2","Bob","b@example.com", LocalDate.now());
        ss.addStudent(s1); ss.addStudent(s2);
        assertEquals(1, ss.search("Alice").size());
    }

    @Test
    void updateStudent() {
        StudentService ss = new StudentService();
        Student original = StudentFactory.createUndergraduate("U3","Before","b@example.com", LocalDate.of(2000,1,1));
        ss.addStudent(original);

        Student updated = StudentFactory.createUndergraduate("U3","After","after@example.com", LocalDate.of(2001,2,2));
        assertTrue(ss.updateStudent("U3", updated));

        Student found = ss.findById("U3").orElseThrow();
        assertEquals("After", found.getName());
        assertEquals("after@example.com", found.getEmail());
        assertEquals(LocalDate.of(2001,2,2), found.getDateOfBirth());
    }

    @Test
    void deleteStudent() {
        StudentService ss = new StudentService();
        Student s = StudentFactory.createUndergraduate("U4","ToDelete","d@example.com", LocalDate.now());
        ss.addStudent(s);
        assertTrue(ss.findById("U4").isPresent());
        assertTrue(ss.remove("U4"));
        assertFalse(ss.findById("U4").isPresent());
        // removing again should return false
        assertFalse(ss.remove("U4"));
    }

    @Test
    void searchEmptyAndNullReturnsAll() {
        StudentService ss = new StudentService();
        Student s1 = StudentFactory.createUndergraduate("S1","A","a@example.com", LocalDate.now());
        Student s2 = StudentFactory.createUndergraduate("S2","B","b@example.com", LocalDate.now());
        ss.addStudent(s1); ss.addStudent(s2);

        assertEquals(2, ss.search("").size());
        assertEquals(2, ss.search(null).size());
    }
}
