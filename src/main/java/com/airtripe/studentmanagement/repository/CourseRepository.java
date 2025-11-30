// ...existing code...
package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Course;

import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    Course addCourse(Course c);
    Optional<Course> findById(String id);
    List<Course> findAll();
    List<Course> search(String query);
    boolean remove(String id);
    boolean updateCourse(String id, Course updated);
    void init() throws Exception;
}

