package com.airtripe.studentmanagement.entity;

import com.airtripe.studentmanagement.interfacepkg.Searchable;
import com.airtripe.studentmanagement.interfacepkg.Gradeable;

import java.time.LocalDate;
import java.util.*;

public class Student extends Person implements Searchable, Gradeable {
    private final Map<String, Double> grades = new HashMap<>(); // courseId -> grade

    public Student(String id, String name, String email, LocalDate dateOfBirth) {
        super(id, name, email, dateOfBirth);
    }

    @Override
    public boolean matches(String query) {
        String q = query == null ? "" : query.toLowerCase();
        return id.toLowerCase().contains(q) || name.toLowerCase().contains(q) || email.toLowerCase().contains(q);
    }

    @Override
    public void setGrade(String courseId, double grade) {
        grades.put(courseId, grade);
    }

    @Override
    public Optional<Double> getGrade(String courseId) {
        return Optional.ofNullable(grades.get(courseId));
    }

    public Map<String, Double> getAllGrades() {
        return Collections.unmodifiableMap(grades);
    }
}
