package com.airtripe.studentmanagement.interfacepkg;

import java.util.Optional;

public interface Gradeable {
    void setGrade(String courseId, double grade);
    Optional<Double> getGrade(String courseId);
}
