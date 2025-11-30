package com.airtripe.studentmanagement.service;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class AsyncStudentService {
    private static final Logger logger = LoggerFactory.getLogger(AsyncStudentService.class);

    private final StudentRepository repository;
    private final ExecutorService executor;

    public AsyncStudentService(StudentRepository repository, int threads) {
        this.repository = repository;
        this.executor = Executors.newFixedThreadPool(Math.max(1, threads));
    }

    public Future<Student> addStudentAsync(Student s) {
        return executor.submit(() -> {
            logger.debug("Adding student async: {}", s.getId());
            return repository.addStudent(s);
        });
    }

    public Future<List<Student>> searchAsync(String q) {
        return executor.submit(() -> {
            logger.debug("Searching async: {}", q);
            return repository.search(q);
        });
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }
}

