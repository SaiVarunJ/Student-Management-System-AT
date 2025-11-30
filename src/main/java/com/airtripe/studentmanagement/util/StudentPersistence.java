package com.airtripe.studentmanagement.util;

import com.airtripe.studentmanagement.entity.Student;
import com.airtripe.studentmanagement.factory.StudentFactory;
import com.airtripe.studentmanagement.exception.PersistenceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StudentPersistence {
    private static final Logger logger = LoggerFactory.getLogger(StudentPersistence.class);
    private final ObjectMapper mapper;

    public StudentPersistence() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // support LocalDate
    }

    public List<Student> load(String filePath) {
        File f = new File(filePath);
        if (!f.exists()) {
            logger.info("Data file {} does not exist, returning empty list", filePath);
            return new ArrayList<>();
        }
        try {
            List<StudentDTO> dtos;
            try (var is = mapper.getFactory().createParser(f)) {
                // use diamond operator to avoid explicit type repetition
                dtos = mapper.readValue(is, new TypeReference<>() {});
            }
            // map DTOs to domain Student objects
            return dtos.stream().map(d -> {
                if (d.getThesisTitle() != null && !d.getThesisTitle().isBlank()) {
                    return StudentFactory.createGraduate(d.getId(), d.getName(), d.getEmail(), d.getDateOfBirth(), d.getThesisTitle());
                }
                return StudentFactory.createUndergraduate(d.getId(), d.getName(), d.getEmail(), d.getDateOfBirth());
            }).collect(Collectors.toList());
        } catch (IOException e) {
            throw new PersistenceException("Failed to load student data from " + filePath, e);
        }
    }

    public void save(List<Student> students, String filePath) {
        List<StudentDTO> dtos = students.stream().map(s -> {
            String thesis = null;
            if (s instanceof com.airtripe.studentmanagement.entity.GraduateStudent) {
                thesis = ((com.airtripe.studentmanagement.entity.GraduateStudent) s).getThesisTitle();
            }
            return new StudentDTO(s.getId(), s.getName(), s.getEmail(), s.getDateOfBirth(), thesis);
        }).collect(Collectors.toList());

        File f = new File(filePath);
        try {
            // ensure parent directories exist; log if creation failed but continue (may already exist)
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean created = parent.mkdirs();
                if (!created) logger.warn("Could not create parent directories for {}", filePath);
            }
            // use Jackson convenience method to write directly to file
            mapper.writeValue(f, dtos);
            logger.info("Saved {} students to {}", dtos.size(), filePath);
        } catch (IOException e) {
            throw new PersistenceException("Failed to save student data to " + filePath, e);
        }
    }
}
