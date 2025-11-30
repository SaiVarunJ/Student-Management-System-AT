package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.util.ConfigSingleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncStudentServiceTest {
    private StudentRepositoryJdbc repo;

    @BeforeEach
    void setup() throws Exception {
        repo = new StudentRepositoryJdbc();
        repo.init();
        try (Connection c = ConfigSingleton.getInstance().getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("DELETE FROM students");
        }
    }

    @Test
    void concurrentAddsAndSearch() throws Exception {
        AsyncStudentService async = new AsyncStudentService(repo, 4);
        List<Future<Student>> futures = new ArrayList<>();

        int count = 50;
        IntStream.range(0, count).forEach(i -> {
            Student s = StudentFactory.createUndergraduate("A" + i, "Name" + i, "user" + i + "@example.com", LocalDate.of(1990,1,1).plusDays(i));
            futures.add(async.addStudentAsync(s));
        });

        // wait for all futures
        for (Future<Student> f : futures) {
            Student out = f.get();
            assertNotNull(out);
        }

        // search (async)
        List<Student> all = async.searchAsync("").get();
        assertEquals(count, all.size());

        async.shutdown();
    }
}

