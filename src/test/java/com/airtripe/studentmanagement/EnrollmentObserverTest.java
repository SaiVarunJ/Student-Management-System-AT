package com.airtripe.studentmanagement;

import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.observer.GradeNotificationService;
import com.airtripe.studentmanagement.observer.GradeNotificationListener;
import com.airtripe.studentmanagement.service.EnrollmentService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class EnrollmentObserverTest {
    @Test
    void gradeAssignmentNotifiesListener() throws Exception {
        Student s = StudentFactory.createUndergraduate("T10", "Obs", "obs@example.com", LocalDate.of(2000,1,1));
        Course c = new Course("CT1", "Observer Test", 3);

        GradeNotificationService svc = new GradeNotificationService();

        CountDownLatch latch = new CountDownLatch(1);
        final double[] received = new double[1];

        // register a simple listener that captures the grade and counts down
        svc.registerGlobalListener((enrollment, grade) -> {
            received[0] = grade != null ? grade : Double.NaN;
            latch.countDown();
        });

        EnrollmentService es = new EnrollmentService(svc);
        var enrollment = es.enroll(s, c);

        enrollment.setGrade(91.0);

        boolean ok = latch.await(1, TimeUnit.SECONDS);
        assertTrue(ok, "Listener was not notified in time");
        assertEquals(91.0, received[0], 0.0001);
    }
}

