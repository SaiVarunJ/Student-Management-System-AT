package com.airtripe.studentmanagement.entity;

import java.time.LocalDate;

public abstract class Person {
    protected final String id;
    protected final String name;
    protected final String email;
    protected final LocalDate dateOfBirth;

    public Person(String id, String name, String email, LocalDate dateOfBirth) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    @Override
    public String toString() {
        return String.format("%s[id=%s,name=%s,email=%s,dob=%s]", getClass().getSimpleName(), id, name, email, dateOfBirth);
    }
}
