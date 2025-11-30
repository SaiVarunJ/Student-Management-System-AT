package com.airtripe.studentmanagement;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.entity.Course;
import com.airtripe.studentmanagement.service.CourseService;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import com.airtripe.studentmanagement.util.InputValidator;
import com.airtripe.studentmanagement.util.DateUtil;
import com.airtripe.studentmanagement.util.StudentPersistence;
import com.airtripe.studentmanagement.service.StudentRepositoryJdbc;
import com.airtripe.studentmanagement.service.StudentRepository;
import com.airtripe.studentmanagement.service.EnrollmentService;
import com.airtripe.studentmanagement.observer.GradeNotificationService;
import com.airtripe.studentmanagement.observer.GradeNotificationListener;

import org.h2.tools.Server;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        ConfigSingleton.getInstance(); // initialize config singleton

        // initialize JDBC repository and REST server
        StudentRepositoryJdbc repo = new StudentRepositoryJdbc();
        repo.init();

        // file persistence
        StudentPersistence persistence = new StudentPersistence();
        String dataFile = ConfigSingleton.getInstance().getDataFilePath();
        try {
            List<Student> loaded = persistence.load(dataFile);
            loaded.forEach(repo::addStudent);
        } catch (Exception e) {
            System.out.println("Warning: failed to load persisted students: " + e.getMessage());
        }

        // start H2 web console so DB can be inspected at http://localhost:8082
        Server webServer = null;
        try {
            webServer = Server.createWebServer("-webPort", "8082", "-webAllowOthers").start();
            System.out.println("H2 web console started at: http://localhost:8082 (use JDBC URL: " + ConfigSingleton.getInstance().getJdbcUrl() + ")");
        } catch (Exception e) {
            System.out.println("Failed to start H2 web console: " + e.getMessage());
        }

        // seed some data into DB if none exists
        if (repo.findAll().isEmpty()) {
            Student s1 = StudentFactory.createUndergraduate("S001", "Alice", "alice@example.com", LocalDate.of(2002,5,1));
            Student s2 = StudentFactory.createUndergraduate("S002", "Bob", "bob@example.com", LocalDate.of(1998,3,12));
            Student g1 = StudentFactory.createGraduate("G001", "Carol", "carol@example.com", LocalDate.of(1995,4,10), "Quantum Computing Thesis");
            repo.addStudent(s1);
            repo.addStudent(s2);
            repo.addStudent(g1);
        }

        CourseService courseService = new CourseService();
        Course c1 = new Course("C101", "Data Structures", 4);
        Course c2 = new Course("C102", "Algorithms", 4);
        courseService.addCourse(c1);
        courseService.addCourse(c2);

        // Observer: setup notification service and register a listener
        GradeNotificationService notificationService = new GradeNotificationService();
        GradeNotificationListener consoleListener = new GradeNotificationListener();
        notificationService.registerGlobalListener(consoleListener);

        // Enrollment service that attaches listeners on enroll
        EnrollmentService enrollmentService = new EnrollmentService(notificationService);

        // demo: enroll S001 into C101 and assign a grade to trigger notification
        var maybeS1 = repo.findById("S001");
        if (maybeS1.isPresent()) {
            var enrollment = enrollmentService.enroll(maybeS1.get(), c1);
            enrollment.setGrade(88.5);
        }

        // start REST server in background
        RestServer rest = new RestServer(repo, 8000);
        Thread restThread = new Thread(rest::start, "rest-server");
        restThread.setDaemon(true);
        restThread.start();

        System.out.println("Student Management System (console + REST)");
        System.out.println("REST server listening on port 8000 (endpoint: /students)");

        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMenu();
                System.out.print("Choose an option: ");
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1":
                        addStudentFlow(scanner, repo);
                        break;
                    case "2":
                        viewAllStudents(repo);
                        break;
                    case "3":
                        updateStudentFlow(scanner, repo);
                        break;
                    case "4":
                        deleteStudentFlow(scanner, repo);
                        break;
                    case "5":
                        searchStudentFlow(scanner, repo);
                        break;
                    case "6":
                        running = false;
                        System.out.println("Goodbye.");
                        break;
                    default:
                        System.out.println("Invalid option. Try again.");
                }
            }
        } finally {
            // on exit save students to data file
            try {
                persistence.save(repo.findAll(), dataFile);
            } catch (Exception e) {
                System.out.println("Error saving students to " + dataFile + ": " + e.getMessage());
            }
            rest.stop();
            if (webServer != null) {
                webServer.stop();
                System.out.println("H2 web console stopped");
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== Student Management System ===");
        System.out.println("1) Add Student");
        System.out.println("2) View All Students");
        System.out.println("3) Update Student");
        System.out.println("4) Delete Student");
        System.out.println("5) Search Students (by id/name/email)");
        System.out.println("6) Exit");
    }

    private static void addStudentFlow(Scanner scanner, StudentRepository repo) {
        System.out.print("Enter student id: ");
        String id = scanner.nextLine().trim();
        if (!InputValidator.notNullOrEmpty(id)) {
            System.out.println("Invalid id.");
            return;
        }
        System.out.print("Enter name: ");
        String name = scanner.nextLine().trim();
        if (!InputValidator.notNullOrEmpty(name)) {
            System.out.println("Invalid name.");
            return;
        }
        System.out.print("Enter email: ");
        String email = scanner.nextLine().trim();
        if (!InputValidator.isValidEmail(email)) {
            System.out.println("Invalid email format.");
            return;
        }
        System.out.print("Enter date of birth (YYYY-MM-DD): ");
        String dobStr = scanner.nextLine().trim();
        LocalDate dob;
        try {
            dob = LocalDate.parse(dobStr);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format.");
            return;
        }

        Student s = StudentFactory.createUndergraduate(id, name, email, dob);
        repo.addStudent(s);
        System.out.println("Student added: " + s);
    }

    private static void viewAllStudents(StudentRepository repo) {
        List<Student> all = repo.findAll();
        if (all.isEmpty()) {
            System.out.println("No students.");
            return;
        }
        System.out.println("Students:");
        all.forEach(s -> System.out.println(formatStudent(s)));
    }

    private static void updateStudentFlow(Scanner scanner, StudentRepository repo) {
        System.out.print("Enter id of student to update: ");
        String id = scanner.nextLine().trim();
        Optional<Student> opt = repo.findById(id);
        if (opt.isEmpty()) {
            System.out.println("Student not found.");
            return;
        }
        Student existing = opt.get();
        System.out.println("Leave blank to keep current value.");
        System.out.print("Name [" + existing.getName() + "]: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) name = existing.getName();
        System.out.print("Email [" + existing.getEmail() + "]: ");
        String email = scanner.nextLine().trim();
        if (email.isEmpty()) email = existing.getEmail();
        if (!InputValidator.isValidEmail(email)) {
            System.out.println("Invalid email format.");
            return;
        }
        System.out.print("Date of birth (YYYY-MM-DD) [" + DateUtil.format(existing.getDateOfBirth()) + "]: ");
        String dobStr = scanner.nextLine().trim();
        LocalDate dob = existing.getDateOfBirth();
        if (!dobStr.isEmpty()) {
            try {
                dob = LocalDate.parse(dobStr);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format.");
                return;
            }
        }
        Student updated = StudentFactory.createUndergraduate(id, name, email, dob);
        boolean ok = repo.updateStudent(id, updated);
        if (ok) System.out.println("Student updated: " + updated);
        else System.out.println("Update failed.");
    }

    private static void deleteStudentFlow(Scanner scanner, StudentRepository repo) {
        System.out.print("Enter id of student to delete: ");
        String id = scanner.nextLine().trim();
        boolean removed = repo.remove(id);
        System.out.println(removed ? "Student removed." : "Student not found.");
    }

    private static void searchStudentFlow(Scanner scanner, StudentRepository repo) {
        System.out.print("Enter search query (id/name/email): ");
        String q = scanner.nextLine().trim();
        List<Student> results = repo.search(q);
        if (results.isEmpty()) System.out.println("No matches.");
        else results.forEach(s -> System.out.println(formatStudent(s)));
    }

    private static String formatStudent(Student s) {
        return String.format("%s | id=%s | email=%s | dob=%s", s.getClass().getSimpleName(), s.getId(), s.getEmail(), DateUtil.format(s.getDateOfBirth()));
    }
}
