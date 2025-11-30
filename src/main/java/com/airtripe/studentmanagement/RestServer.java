package com.airtripe.studentmanagement;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.service.StudentRepository;
import com.airtripe.studentmanagement.util.ConfigSingleton;
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

public class RestServer {
    private static final Logger logger = LoggerFactory.getLogger(RestServer.class);
    private final StudentRepository repository;
    private final HttpServer server;
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public RestServer(StudentRepository repository, int port) throws IOException {
        this.repository = repository;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/students", this::handleStudents);
        server.createContext("/metrics", this::handleMetrics);
        // provide a convenient redirect endpoint for the H2 web console
        server.createContext("/h2-console", this::handleH2ConsoleRedirect);
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
            } else if ("POST".equalsIgnoreCase(method) && (parts.length == 2 || parts.length==0)) {
                // create
                Student s = readStudent(ex.getRequestBody());
                repository.addStudent(s);
                writeJson(ex, 201, s);
                return;
            } else if ("PUT".equalsIgnoreCase(method) && parts.length == 3) {
                String id = parts[2];
                Student s = readStudent(ex.getRequestBody());
                boolean ok = repository.updateStudent(id, s);
                if (ok) writeJson(ex, 200, s); else sendEmpty(ex, 404);
                return;
            } else if ("DELETE".equalsIgnoreCase(method) && parts.length == 3) {
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
