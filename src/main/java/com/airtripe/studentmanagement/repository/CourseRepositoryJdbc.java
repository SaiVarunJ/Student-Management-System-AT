// ...existing code...
package com.airtripe.studentmanagement.repository;

import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import com.airtripe.studentmanagement.util.MetricsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseRepositoryJdbc implements CourseRepository {
    private static final Logger logger = LoggerFactory.getLogger(CourseRepositoryJdbc.class);
    private final ConfigSingleton config = ConfigSingleton.getInstance();
    private final MetricsCollector metrics = new MetricsCollector();

    @Override
    public void init() throws Exception {
        try (Connection conn = config.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS courses (id VARCHAR(100) PRIMARY KEY, name VARCHAR(255), credits INT)");
            logger.info("courses table ensured");
        }
    }

    @Override
    public Course addCourse(Course c) {
        long start = System.nanoTime();
        String sql = "MERGE INTO courses (id, name, credits) KEY(id) VALUES (?, ?, ?)";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getId());
            ps.setString(2, c.getName());
            ps.setInt(3, c.getCredits());
            ps.executeUpdate();
            return c;
        } catch (SQLException e) {
            logger.error("Failed to add course {}", c.getId(), e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.add", System.nanoTime() - start);
        }
    }

    @Override
    public Optional<Course> findById(String id) {
        long start = System.nanoTime();
        String sql = "SELECT id, name, credits FROM courses WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Failed to find course {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.findById", System.nanoTime() - start);
        }
        return Optional.empty();
    }

    @Override
    public List<Course> findAll() {
        long start = System.nanoTime();
        String sql = "SELECT id, name, credits FROM courses ORDER BY id";
        List<Course> list = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            logger.error("Failed to fetch all courses", e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.findAll", System.nanoTime() - start);
        }
    }

    @Override
    public List<Course> search(String query) {
        long start = System.nanoTime();
        if (query == null || query.isEmpty()) return findAll();
        String q = "%" + query.toLowerCase() + "%";
        String sql = "SELECT id, name, credits FROM courses WHERE LOWER(id) LIKE ? OR LOWER(name) LIKE ? ORDER BY id";
        List<Course> list = new ArrayList<>();
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            logger.error("Course search failed", e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.search", System.nanoTime() - start);
        }
    }

    @Override
    public boolean remove(String id) {
        long start = System.nanoTime();
        String sql = "DELETE FROM courses WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Delete failed for course {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.delete", System.nanoTime() - start);
        }
    }

    @Override
    public boolean updateCourse(String id, Course updated) {
        long start = System.nanoTime();
        String sql = "UPDATE courses SET name = ?, credits = ? WHERE id = ?";
        try (Connection conn = config.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, updated.getName());
            ps.setInt(2, updated.getCredits());
            ps.setString(3, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Update failed for course {}", id, e);
            throw new RuntimeException(e);
        } finally {
            metrics.record("courses.update", System.nanoTime() - start);
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String name = rs.getString("name");
        int credits = rs.getInt("credits");
        return new Course(id, name, credits);
    }

    public MetricsCollector getMetricsCollector() { return metrics; }
}

