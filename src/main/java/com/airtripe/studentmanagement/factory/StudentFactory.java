package com.airtripe.studentmanagement.factory;

import com.airtripe.studentmanagement.entity.GraduateStudent;
import com.airtripe.studentmanagement.entity.Student;

import java.time.LocalDate;

public class StudentFactory {
    public static Student createUndergraduate(String id, String name, String email, LocalDate dob) {
        return new Student(id, name, email, dob);
    }

    public static GraduateStudent createGraduate(String id, String name, String email, LocalDate dob, String thesisTitle) {
        return new GraduateStudent(id, name, email, dob, thesisTitle);
    }
}
