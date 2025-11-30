package com.airtripe.studentmanagement;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.entity.Enrollment;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.repository.StudentRepository;
import com.airtripe.studentmanagement.repository.CourseRepository;
import com.airtripe.studentmanagement.repository.EnrollmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RestServer {
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private final StudentRepository repository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // backward-compatible constructor (students-only)
    public RestServer(StudentRepository repository, int port) throws IOException {
        this(repository, null, null, port);
    }

    // full constructor with optional course/enrollment repos
    public RestServer(StudentRepository repository, CourseRepository courseRepository, EnrollmentRepository enrollmentRepository, int port) throws IOException {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/students", this::handleStudents);
        server.createContext("/metrics", this::handleMetrics);
        server.createContext("/h2-console", this::handleH2ConsoleRedirect);
        if (courseRepository != null) server.createContext("/courses", this::handleCourses);
        if (enrollmentRepository != null) server.createContext("/enrollments", this::handleEnrollments);
    }

    public void start() {
        server.start();
        logger.info("REST server started on {}", server.getAddress());
    }

    public void stop() {
        server.stop(0);
        logger.info("REST server stopped");
    }

    private void handleH2ConsoleRedirect(HttpExchange ex) throws IOException {
        try {
            // Default H2 web console URL; if you change the H2 port in Main, update ConfigSingleton or this URL accordingly
            String h2Url = System.getProperty("sms.h2.console.url", "http://localhost:8082/");
            ex.getResponseHeaders().add("Location", h2Url);
            ex.sendResponseHeaders(302, -1);
        } catch (Exception e) {
            logger.error("Failed to redirect to H2 console", e);
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        } finally {
            ex.close();
        }
    }

    private void handleStudents(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();
            String path = uri.getPath();
            // /students or /students/{id}
            String[] parts = path.split("/");
            if ("GET".equalsIgnoreCase(method)) {
                if (parts.length == 2 || parts.length == 0) {
                    // list all or search query
                    String q = getQueryParam(uri.getQuery(), "q");
                    List<Student> list = repository.search(q);
                    writeJson(ex, 200, list);
                    return;
                } else if (parts.length == 3) {
                    String id = parts[2];
                    repository.findById(id).ifPresentOrElse(s -> {
                        try { writeJson(ex, 200, s); } catch (IOException e) { throw new RuntimeException(e); }
                    }, () -> {
                        try { sendEmpty(ex, 404); } catch (IOException e) { throw new RuntimeException(e); }
                    });
                    return;
                }
            }
            else if ("POST".equalsIgnoreCase(method) && (parts.length == 2 || parts.length==0)) {
                // create
                Student s = readStudent(ex.getRequestBody());
                repository.addStudent(s);
                writeJson(ex, 201, s);
                return;
            }
            else if ("PUT".equalsIgnoreCase(method) && parts.length == 3) {
                String id = parts[2];
                Student s = readStudent(ex.getRequestBody());
                boolean ok = repository.updateStudent(id, s);
                if (ok) writeJson(ex, 200, s); else sendEmpty(ex, 404);
                return;
            }
            else if ("DELETE".equalsIgnoreCase(method) && parts.length == 3) {
                String id = parts[2];
                boolean ok = repository.remove(id);
                sendEmpty(ex, ok ? 204 : 404);
                return;
            }
            sendEmpty(ex, 405);
        } catch (Exception e) {
            logger.error("Error handling students request", e);
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleMetrics(HttpExchange ex) throws IOException {
        // simple metrics: number of students
        try {
            List<Student> all = repository.findAll();
            Map<String, Object> m = Map.of("studentCount", all.size());
            writeJson(ex, 200, m);
        } catch (Exception e) {
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleCourses(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();
            String path = uri.getPath();
            String[] parts = path.split("/");
            if ("GET".equalsIgnoreCase(method)) {
                if (parts.length == 2 || parts.length == 0) {
                    String q = getQueryParam(uri.getQuery(), "q");
                    List<Course> list = courseRepository.search(q);
                    writeJson(ex, 200, list);
                    return;
                } else if (parts.length == 3) {
                    String id = parts[2];
                    courseRepository.findById(id).ifPresentOrElse(c -> {
                        try { writeJson(ex, 200, c); } catch (IOException e) { throw new RuntimeException(e); }
                    }, () -> {
                        try { sendEmpty(ex, 404); } catch (IOException e) { throw new RuntimeException(e); }
                    });
                    return;
                }
            } else if ("POST".equalsIgnoreCase(method) && (parts.length == 2 || parts.length == 0)) {
                Course c = readCourse(ex.getRequestBody());
                courseRepository.addCourse(c);
                writeJson(ex, 201, c);
                return;
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 3) {
                String id = parts[2];
                Course c = readCourse(ex.getRequestBody());
                boolean ok = courseRepository.updateCourse(id, c);
                if (ok) writeJson(ex, 200, c); else sendEmpty(ex, 404);
                return;
            } else if ("DELETE".equalsIgnoreCase(method) && parts.length == 3) {
                String id = parts[2];
                boolean ok = courseRepository.remove(id);
                sendEmpty(ex, ok ? 204 : 404);
                return;
            }
            sendEmpty(ex, 405);
        } catch (Exception e) {
            logger.error("Error handling courses request", e);
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleEnrollments(HttpExchange ex) throws IOException {
        try {
            String method = ex.getRequestMethod();
            URI uri = ex.getRequestURI();
            String path = uri.getPath();
            String[] parts = path.split("/");
            // GET /enrollments -> list all or query by studentId/courseId
            if ("GET".equalsIgnoreCase(method)) {
                String studentId = getQueryParam(uri.getQuery(), "studentId");
                String courseId = getQueryParam(uri.getQuery(), "courseId");
                if (studentId != null) {
                    List<Enrollment> list = enrollmentRepository.findByStudentId(studentId);
                    writeJson(ex, 200, list);
                    return;
                } else if (courseId != null) {
                    List<Enrollment> list = enrollmentRepository.findByCourseId(courseId);
                    writeJson(ex, 200, list);
                    return;
                } else if (parts.length >= 4) {
                    // GET /enrollments/{studentId}/{courseId}
                    String sid = parts[2];
                    String cid = parts[3];
                    Optional<Enrollment> eOpt = enrollmentRepository.find(new Student(sid, "", "", LocalDate.now()), new Course(cid, "", 0));
                    eOpt.ifPresentOrElse(e -> {
                        try { writeJson(ex, 200, e); } catch (IOException ioException) { throw new RuntimeException(ioException); }
                    }, () -> {
                        try { sendEmpty(ex, 404); } catch (IOException ioException) { throw new RuntimeException(ioException); }
                    });
                    return;
                } else {
                    List<Enrollment> all = enrollmentRepository.findAll();
                    writeJson(ex, 200, all);
                    return;
                }
            } else if ("POST".equalsIgnoreCase(method) && (parts.length == 2 || parts.length == 0)) {
                // create enrollment with JSON { "studentId": "S001", "courseId": "C101", "enrolledOn":"YYYY-MM-DD" }
                Map<?,?> map = mapper.readValue(ex.getRequestBody(), Map.class);
                String sid = (String) map.get("studentId");
                String cid = (String) map.get("courseId");
                String enrolledOn = (String) map.get("enrolledOn");
                if (sid == null || cid == null) { writeJson(ex, 400, Map.of("error", "studentId and courseId required")); return; }
                Optional<Student> sOpt = repository.findById(sid);
                Optional<Course> cOpt = courseRepository.findById(cid);
                if (sOpt.isEmpty() || cOpt.isEmpty()) { sendEmpty(ex, 404); return; }
                LocalDate ld = enrolledOn == null ? LocalDate.now() : LocalDate.parse(enrolledOn);
                Enrollment e = new Enrollment(sOpt.get(), cOpt.get(), ld);
                enrollmentRepository.add(e);
                writeJson(ex, 201, e);
                return;
            } else if ("PUT".equalsIgnoreCase(method) && parts.length >= 4) {
                // PUT /enrollments/{studentId}/{courseId} with body { "grade": 9.5 }
                String sid = parts[2];
                String cid = parts[3];
                Map<?,?> map = mapper.readValue(ex.getRequestBody(), Map.class);
                Object gradeObj = map.get("grade");
                if (gradeObj == null) { writeJson(ex, 400, Map.of("error", "grade is required")); return; }
                double grade = ((Number) gradeObj).doubleValue();
                // locate enrollment
                Optional<Student> sOpt = repository.findById(sid);
                Optional<Course> cOpt = courseRepository.findById(cid);
                if (sOpt.isEmpty() || cOpt.isEmpty()) { sendEmpty(ex, 404); return; }
                Optional<Enrollment> enOpt = enrollmentRepository.find(sOpt.get(), cOpt.get());
                if (enOpt.isEmpty()) { sendEmpty(ex, 404); return; }
                Enrollment e = enOpt.get();
                e.setGrade(grade);
                enrollmentRepository.add(e); // persist updated grade
                writeJson(ex, 200, e);
                return;
            } else if ("DELETE".equalsIgnoreCase(method) && parts.length >= 4) {
                String sid = parts[2];
                String cid = parts[3];
                boolean ok = enrollmentRepository.remove(sid, cid);
                sendEmpty(ex, ok ? 204 : 404);
                return;
            }
            sendEmpty(ex, 405);
        } catch (Exception e) {
            logger.error("Error handling enrollments request", e);
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private Student readStudent(InputStream is) throws IOException {
        Map<?,?> map = mapper.readValue(is, Map.class);
        String id = (String) map.get("id");
        String name = (String) map.get("name");
        String email = (String) map.get("email");
        String dob = (String) map.get("dateOfBirth");
        // accept either camelCase or snake_case for thesis
        Object thesisObj = map.get("thesisTitle");
        if (thesisObj == null) thesisObj = map.get("thesis_title");
        String thesis = thesisObj == null ? null : thesisObj.toString();
        LocalDate ld = dob == null ? LocalDate.now() : LocalDate.parse(dob);
        if (thesis != null && !thesis.isBlank()) {
            return StudentFactory.createGraduate(id, name, email, ld, thesis);
        }
        return StudentFactory.createUndergraduate(id, name, email, ld);
    }

    private Course readCourse(InputStream is) throws IOException {
        Map<?,?> map = mapper.readValue(is, Map.class);
        String id = (String) map.get("id");
        String name = (String) map.get("name");
        Integer credits = map.get("credits") == null ? 0 : ((Number)map.get("credits")).intValue();
        return new Course(id, name, credits);
    }

    private void writeJson(HttpExchange ex, int code, Object obj) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(obj);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }

    private String getQueryParam(String query, String key) {
        if (query == null || query.isEmpty()) return null;
        return java.util.Arrays.stream(query.split("&"))
                .map(s -> s.split("=",2))
                .filter(parts -> parts.length == 2 && parts[0].equals(key))
                .map(parts -> parts[1])
                .findFirst().orElse(null);
    }
}
