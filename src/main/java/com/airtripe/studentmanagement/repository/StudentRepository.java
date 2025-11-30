package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Student;

import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    Student addStudent(Student s);
    Optional<Student> findById(String id);
    List<Student> findAll();
    List<Student> search(String query);
    boolean remove(String id);
    boolean updateStudent(String id, Student updated);
    void init() throws Exception; // initialize schema if needed
}

