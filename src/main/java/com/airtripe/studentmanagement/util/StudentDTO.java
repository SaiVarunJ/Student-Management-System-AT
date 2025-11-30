package com.airtripe.studentmanagement.util;

import java.time.LocalDate;

public class StudentDTO {
    private String id;
    private String name;
    private String email;
    private LocalDate dateOfBirth;
    private String thesisTitle; // null for undergraduates

    // Jackson requires a no-arg constructor
    public StudentDTO() {}

    public StudentDTO(String id, String name, String email, LocalDate dateOfBirth, String thesisTitle) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.thesisTitle = thesisTitle;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getThesisTitle() { return thesisTitle; }
    public void setThesisTitle(String thesisTitle) { this.thesisTitle = thesisTitle; }
}

