package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import com.airtripe.studentmanagement.util.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentRepositoryJdbc implements StudentRepository {
    private static final Logger logger = LoggerFactory.getLogger(StudentRepositoryJdbc.class);

    private final ConfigSingleton config = ConfigSingleton.getInstance();
    private final MetricsCollector metrics = new MetricsCollector();

    @Override
    public void init() throws Exception {
        try (Connection conn = config.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS students (id VARCHAR(100) PRIMARY KEY, name VARCHAR(255), email VARCHAR(255), dob DATE, thesis_title VARCHAR(255))");
            logger.info("students table ensured");
        }
    }

    @Override
    public Student addStudent(Student s) {
        long start = System.nanoTime();
        String sql = "MERGE INTO students (id, name, email, dob, thesis_title) KEY(id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getId());
            ps.setString(2, s.getName());
            ps.setString(3, s.getEmail());
            ps.setDate(4, Date.valueOf(s.getDateOfBirth()));
            String thesis = null;
            if (s instanceof com.airtripe.studentmanagement.entity.GraduateStudent) {
                thesis = ((com.airtripe.studentmanagement.entity.GraduateStudent) s).getThesisTitle();
            }
            ps.setString(5, thesis);
            ps.executeUpdate();
            return s;
        } catch (SQLException e) {
            logger.error("Failed to add student {}", s.getId(), e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.add", System.nanoTime() - start);
        }
    }

    @Override
    public Optional<Student> findById(String id) {
        long start = System.nanoTime();
        String sql = "SELECT id, name, email, dob, thesis_title FROM students WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find student {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.findById", System.nanoTime() - start);
        }
        return Optional.empty();
    }

    @Override
    public List<Student> findAll() {
        long start = System.nanoTime();
        String sql = "SELECT id, name, email, dob, thesis_title FROM students ORDER BY id";
        List<Student> list = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            logger.error("Failed to fetch all students", e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.findAll", System.nanoTime() - start);
        }
    }

    @Override
    public List<Student> search(String query) {
        long start = System.nanoTime();
        if (query == null || query.isEmpty()) return findAll();
        String q = "%" + query.toLowerCase() + "%";
        String sql = "SELECT id, name, email, dob, thesis_title FROM students WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? OR LOWER(email) LIKE ? ORDER BY id";
        List<Student> list = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            logger.error("Search failed", e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.search", System.nanoTime() - start);
        }
    }

    @Override
    public boolean remove(String id) {
        long start = System.nanoTime();
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Delete failed for {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.delete", System.nanoTime() - start);
        }
    }

    @Override
    public boolean updateStudent(String id, Student updated) {
        long start = System.nanoTime();
        String sql = "UPDATE students SET name = ?, email = ?, dob = ?, thesis_title = ? WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updated.getName());
            ps.setString(2, updated.getEmail());
            ps.setDate(3, Date.valueOf(updated.getDateOfBirth()));
            String thesis = null;
            if (updated instanceof com.airtripe.studentmanagement.entity.GraduateStudent) {
                thesis = ((com.airtripe.studentmanagement.entity.GraduateStudent) updated).getThesisTitle();
            }
            ps.setString(4, thesis);
            ps.setString(5, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Update failed for {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("students.update", System.nanoTime() - start);
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        Date dob = rs.getDate("dob");
        LocalDate ld = dob.toLocalDate();
        String thesis = rs.getString("thesis_title");
        if (thesis != null && !thesis.isEmpty()) {
            return StudentFactory.createGraduate(id, name, email, ld, thesis);
        }
        return StudentFactory.createUndergraduate(id, name, email, ld);
    }

    // expose metrics for tests/monitoring
    public MetricsCollector getMetricsCollector() { return metrics; }
}
