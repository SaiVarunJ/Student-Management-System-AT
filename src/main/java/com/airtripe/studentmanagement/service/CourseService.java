package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Course;

import java.util.*;
import java.util.stream.Collectors;

public class CourseService {
    private final Map<String, Course> courses = new LinkedHashMap<>();

    public Course addCourse(Course c) {
        courses.put(c.getId(), c);
        return c;
    }

    public Optional<Course> findById(String id) { return Optional.ofNullable(courses.get(id)); }

    public List<Course> findAll() { return new ArrayList<>(courses.values()); }

    public List<Course> searchByName(String name) {
        if (name == null || name.isEmpty()) return findAll();
        String q = name.toLowerCase();
        return courses.values().stream().filter(c -> c.getName().toLowerCase().contains(q)).collect(Collectors.toList());
    }
}
