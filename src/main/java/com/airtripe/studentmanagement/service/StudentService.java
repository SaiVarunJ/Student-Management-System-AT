package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.exception.StudentNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

public class StudentService {
    // Use ArrayList for dynamic data management as required
    private final List<Student> students = new ArrayList<>();

    public Student addStudent(Student s) {
        // if student with same id exists, replace it
        findById(s.getId()).ifPresent(existing -> students.remove(existing));
        students.add(s);
        return s;
    }

    public Optional<Student> findById(String id) {
        if (id == null) return Optional.empty();
        return students.stream().filter(s -> id.equals(s.getId())).findFirst();
    }

    public Student getByIdOrThrow(String id) {
        return findById(id).orElseThrow(() -> new StudentNotFoundException(id));
    }

    public List<Student> search(String query) {
        if (query == null || query.isEmpty()) return findAll();
        String q = query.toLowerCase();
        return students.stream()
                .filter(s -> s.matches(q))
                .collect(Collectors.toList());
    }

    public List<Student> findAll() {
        return new ArrayList<>(students);
    }

    public boolean remove(String id) {
        Optional<Student> found = findById(id);
        found.ifPresent(students::remove);
        return found.isPresent();
    }

    public boolean updateStudent(String id, Student updated) {
        Optional<Student> found = findById(id);
        if (found.isEmpty()) return false;
        int idx = students.indexOf(found.get());
        students.set(idx, updated);
        return true;
    }
}
