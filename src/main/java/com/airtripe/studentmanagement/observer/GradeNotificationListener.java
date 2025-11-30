package com.airtripe.studentmanagement.observer;

import com.airtripe.studentmanagement.entity.Enrollment;

public class GradeNotificationListener implements Enrollment.GradeListener {
    @Override
    public void onGradeAssigned(Enrollment enrollment, Double grade) {
        System.out.println("Notification: grade " + grade + " assigned to student " + enrollment.getStudent().getId() + " for course " + enrollment.getCourse().getId());
    }
}
