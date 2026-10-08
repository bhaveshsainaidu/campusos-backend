package com.campusos.student.dto;

import jakarta.validation.constraints.*;
import com.campusos.student.Student;

import java.time.LocalDate;

public final class StudentDtos {
    private StudentDtos() {}

    public record CreateStudentRequest(
            @NotBlank @Size(max = 50) String rollNumber,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Email String email,
            @Size(max = 20) String phone,
            Student.Gender gender,
            LocalDate dob,
            @NotNull LocalDate admissionDate,
            @Size(max = 255) String guardianName,
            @Size(max = 500) String address,
            Long departmentId,
            Long batchId) {}

    public record UpdateStudentRequest(
            @Size(max = 255) String name,
            @Size(max = 20) String phone,
            Student.Gender gender,
            LocalDate dob,
            String guardianName,
            String address,
            Long departmentId,
            Long batchId,
            Student.Status status) {}

    public record StudentResponse(
            Long id,
            String rollNumber,
            String name,
            String email,
            String phone,
            Student.Gender gender,
            LocalDate dob,
            LocalDate admissionDate,
            String guardianName,
            String address,
            Student.Status status,
            Long departmentId,
            String departmentCode,
            String departmentName,
            Long batchId,
            String batchName,
            Long userId) {}

    public record CsvImportResult(int imported, int skipped, java.util.List<String> errors) {}
}
