package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Enrollment;
import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.observer.GradeNotificationService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EnrollmentService {
    private final List<Enrollment> enrollments = new ArrayList<>();
    private final GradeNotificationService notificationService;

    public EnrollmentService() {
        this.notificationService = null;
    }

    public EnrollmentService(GradeNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Enrollment enroll(Student student, Course course) {
        Enrollment e = new Enrollment(student, course, LocalDate.now());
        enrollments.add(e);
        if (notificationService != null) {
            notificationService.attachListenersToEnrollment(e);
        }
        return e;
    }

    public List<Enrollment> findByStudentId(String studentId) {
        return enrollments.stream().filter(e -> e.getStudent().getId().equals(studentId)).collect(Collectors.toList());
    }

    public List<Enrollment> findByCourseId(String courseId) {
        return enrollments.stream().filter(e -> e.getCourse().getId().equals(courseId)).collect(Collectors.toList());
    }

    public Optional<Enrollment> find(Student student, Course course) {
        return enrollments.stream().filter(e -> e.getStudent().equals(student) && e.getCourse().equals(course)).findFirst();
    }
}
