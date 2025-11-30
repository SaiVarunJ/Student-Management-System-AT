// ...existing code...
package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Enrollment;
import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.entity.Course;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository {
    Enrollment add(Enrollment e);
    Optional<Enrollment> find(Student student, Course course);
    List<Enrollment> findByStudentId(String studentId);
    List<Enrollment> findByCourseId(String courseId);
    List<Enrollment> findAll();
    boolean remove(String studentId, String courseId);
    void init() throws Exception;
}

