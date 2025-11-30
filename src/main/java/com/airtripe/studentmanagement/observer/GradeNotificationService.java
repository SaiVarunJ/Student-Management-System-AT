package com.airtripe.studentmanagement.observer;

import com.airtripe.studentmanagement.entity.Enrollment;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class GradeNotificationService {
    private final List<Enrollment.GradeListener> globalListeners = new CopyOnWriteArrayList<>();
    private final Map<String, List<Enrollment.GradeListener>> studentListeners = new ConcurrentHashMap<>();
    private final Map<String, List<Enrollment.GradeListener>> courseListeners = new ConcurrentHashMap<>();

    public void registerGlobalListener(Enrollment.GradeListener l) { globalListeners.add(l); }
    public void unregisterGlobalListener(Enrollment.GradeListener l) { globalListeners.remove(l); }

    public void registerStudentListener(String studentId, Enrollment.GradeListener l) {
        studentListeners.computeIfAbsent(studentId, k -> new CopyOnWriteArrayList<>()).add(l);
    }
    public void unregisterStudentListener(String studentId, Enrollment.GradeListener l) {
        List<Enrollment.GradeListener> list = studentListeners.get(studentId);
        if (list != null) list.remove(l);
    }

    public void registerCourseListener(String courseId, Enrollment.GradeListener l) {
        courseListeners.computeIfAbsent(courseId, k -> new CopyOnWriteArrayList<>()).add(l);
    }
    public void unregisterCourseListener(String courseId, Enrollment.GradeListener l) {
        List<Enrollment.GradeListener> list = courseListeners.get(courseId);
        if (list != null) list.remove(l);
    }

    /** Attach relevant listeners to the given enrollment (global, student-specific, course-specific). */
    public void attachListenersToEnrollment(Enrollment e) {
        // add global listeners
        globalListeners.forEach(e::addListener);
        // add listeners for student id
        List<Enrollment.GradeListener> sList = studentListeners.get(e.getStudent().getId());
        if (sList != null) sList.forEach(e::addListener);
        // add listeners for course id
        List<Enrollment.GradeListener> cList = courseListeners.get(e.getCourse().getId());
        if (cList != null) cList.forEach(e::addListener);
    }

}

