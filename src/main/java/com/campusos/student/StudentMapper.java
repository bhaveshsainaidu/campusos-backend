package com.campusos.student;

import com.campusos.student.dto.StudentDtos.StudentResponse;

public final class StudentMapper {
    private StudentMapper() {}

    public static StudentResponse toResponse(Student s) {
        return new StudentResponse(
                s.getId(),
                s.getRollNumber(),
                s.getName(),
                s.getEmail(),
                s.getPhone(),
                s.getGender(),
                s.getDob(),
                s.getAdmissionDate(),
                s.getGuardianName(),
                s.getAddress(),
                s.getStatus(),
                s.getDepartment() != null ? s.getDepartment().getId() : null,
                s.getDepartment() != null ? s.getDepartment().getCode() : null,
                s.getDepartment() != null ? s.getDepartment().getName() : null,
                s.getBatch() != null ? s.getBatch().getId() : null,
                s.getBatch() != null ? s.getBatch().getName() : null,
                s.getUser() != null ? s.getUser().getId() : null);
    }
}
