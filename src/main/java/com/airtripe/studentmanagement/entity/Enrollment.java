package com.airtripe.studentmanagement.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Enrollment {
    private final Student student;
    private final Course course;
    private final LocalDate enrolledOn;
    private Double grade; // nullable
    private final List<GradeListener> listeners = new ArrayList<>();

    public Enrollment(Student student, Course course, LocalDate enrolledOn) {
        this.student = student;
        this.course = course;
        this.enrolledOn = enrolledOn;
    }

    public Student getStudent() { return student; }
    public Course getCourse() { return course; }
    public LocalDate getEnrolledOn() { return enrolledOn; }

    public Optional<Double> getGrade() { return Optional.ofNullable(grade); }

    public void setGrade(Double grade) {
        this.grade = grade;
        // notify observers
        for (GradeListener l : listeners) {
            l.onGradeAssigned(this, grade);
        }
        // also store in student's grade map
        student.setGrade(course.getId(), grade);
    }

    public void addListener(GradeListener listener) { listeners.add(listener); }
    public void removeListener(GradeListener listener) { listeners.remove(listener); }

    public interface GradeListener {
        void onGradeAssigned(Enrollment enrollment, Double grade);
    }

    @Override
    public String toString() {
        return String.format("Enrollment[student=%s,course=%s,enrolledOn=%s,grade=%s]", student.getId(), course.getId(), enrolledOn, grade);
    }
}
