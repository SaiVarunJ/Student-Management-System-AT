package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Enrollment;
import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import com.airtripe.studentmanagement.util.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnrollmentRepositoryJdbc implements EnrollmentRepository {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentRepositoryJdbc.class);
    private final ConfigSingleton config = ConfigSingleton.getInstance();
    private final MetricsCollector metrics = new MetricsCollector();

    // use repository interfaces so this class can accept different implementations
    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;

    // default no-arg constructor for compatibility: uses JDBC implementations
    public EnrollmentRepositoryJdbc() {
        this(new com.airtripe.studentmanagement.repository.StudentRepositoryJdbc(), new com.airtripe.studentmanagement.repository.CourseRepositoryJdbc());
    }

    // constructor for DI/testing
    public EnrollmentRepositoryJdbc(StudentRepository studentRepo, CourseRepository courseRepo) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
    }

    @Override
    public void init() throws Exception {
        // ensure dependent tables
        studentRepo.init();
        courseRepo.init();
        try (Connection conn = config.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS enrollments (student_id VARCHAR(100), course_id VARCHAR(100), enrolled_on DATE, grade DOUBLE, PRIMARY KEY(student_id, course_id))");
            logger.info("enrollments table ensured");
        }
    }

    @Override
    public Enrollment add(Enrollment e) {
        long start = System.nanoTime();
        // ensure student and course exist (MERGE semantics in their repos)
        studentRepo.addStudent(e.getStudent());
        courseRepo.addCourse(e.getCourse());
        String sql = "MERGE INTO enrollments (student_id, course_id, enrolled_on, grade) KEY(student_id, course_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.getStudent().getId());
            ps.setString(2, e.getCourse().getId());
            ps.setDate(3, Date.valueOf(e.getEnrolledOn()));
            if (e.getGrade().isPresent()) ps.setDouble(4, e.getGrade().get()); else ps.setNull(4, Types.DOUBLE);
            ps.executeUpdate();
            return e;
        } catch (SQLException ex) {
            logger.error("Failed to add enrollment {}-{}", e.getStudent().getId(), e.getCourse().getId(), ex);
            throw new RuntimeException(ex);
        } finally {
            metrics.record("enrollments.add", System.nanoTime() - start);
        }
    }

    @Override
    public Optional<Enrollment> find(Student student, Course course) {
        long start = System.nanoTime();
        String sql = "SELECT student_id, course_id, enrolled_on, grade FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, student.getId());
            ps.setString(2, course.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find enrollment {}-{}", student.getId(), course.getId(), e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("enrollments.find", System.nanoTime() - start);
        }
        return Optional.empty();
    }

    @Override
    public List<Enrollment> findByStudentId(String studentId) {
        long start = System.nanoTime();
        String sql = "SELECT student_id, course_id, enrolled_on, grade FROM enrollments WHERE student_id = ? ORDER BY course_id";
        List<Enrollment> out = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            logger.error("Failed to find enrollments for student {}", studentId, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("enrollments.findByStudent", System.nanoTime() - start);
        }
    }

    @Override
    public List<Enrollment> findByCourseId(String courseId) {
        long start = System.nanoTime();
        String sql = "SELECT student_id, course_id, enrolled_on, grade FROM enrollments WHERE course_id = ? ORDER BY student_id";
        List<Enrollment> out = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(mapRow(rs));
            }
            return out;
        } catch (SQLException e) {
            logger.error("Failed to find enrollments for course {}", courseId, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("enrollments.findByCourse", System.nanoTime() - start);
        }
    }

    @Override
    public List<Enrollment> findAll() {
        long start = System.nanoTime();
        String sql = "SELECT student_id, course_id, enrolled_on, grade FROM enrollments ORDER BY student_id, course_id";
        List<Enrollment> out = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            logger.error("Failed to fetch all enrollments", e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("enrollments.findAll", System.nanoTime() - start);
        }
    }

    @Override
    public boolean remove(String studentId, String courseId) {
        long start = System.nanoTime();
        String sql = "DELETE FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            ps.setString(2, courseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Delete failed for enrollment {}-{}", studentId, courseId, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("enrollments.delete", System.nanoTime() - start);
        }
    }

    private Enrollment mapRow(ResultSet rs) throws SQLException {
        String sid = rs.getString("student_id");
        String cid = rs.getString("course_id");
        Date d = rs.getDate("enrolled_on");
        LocalDate ld = d.toLocalDate();
        Double grade = rs.getObject("grade") == null ? null : rs.getDouble("grade");
        Optional<Student> sOpt = studentRepo.findById(sid);
        Optional<Course> cOpt = courseRepo.findById(cid);
        if (sOpt.isEmpty() || cOpt.isEmpty()) {
            throw new SQLException("Enrollment references missing student or course: " + sid + "+" + cid);
        }
        Enrollment e = new Enrollment(sOpt.get(), cOpt.get(), ld);
        if (grade != null) e.setGrade(grade);
        return e;
    }

    public MetricsCollector getMetricsCollector() { return metrics; }
}
