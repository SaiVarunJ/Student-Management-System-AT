package com.airtripe.studentmanagement.entity;

import java.time.LocalDate;

public class GraduateStudent extends Student {
    private final String thesisTitle;

    public GraduateStudent(String id, String name, String email, LocalDate dateOfBirth, String thesisTitle) {
        super(id, name, email, dateOfBirth);
        this.thesisTitle = thesisTitle;
    }

    public String getThesisTitle() {
        return thesisTitle;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" thesis=%s", thesisTitle);
    }
}
