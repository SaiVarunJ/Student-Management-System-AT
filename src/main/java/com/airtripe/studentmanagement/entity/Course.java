package com.airtripe.studentmanagement.entity;

public class Course {
    private final String id;
    private final String name;
    private final int credits;

    public Course(String id, String name, int credits) {
        this.id = id;
        this.name = name;
        this.credits = credits;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getCredits() { return credits; }

    @Override
    public String toString() {
        return String.format("Course[id=%s,name=%s,credits=%d]", id, name, credits);
    }
}
