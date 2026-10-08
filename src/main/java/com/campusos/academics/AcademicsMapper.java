package com.campusos.academics;

import com.campusos.academics.dto.AcademicsDtos.*;

public final class AcademicsMapper {
    private AcademicsMapper() {}

    public static DepartmentResponse toResponse(Department d) {
        return new DepartmentResponse(d.getId(), d.getCode(), d.getName());
    }

    public static SemesterResponse toResponse(Semester s) {
        return new SemesterResponse(s.getId(), s.getName(), s.getAcademicYear(),
                s.getStartDate(), s.getEndDate(), s.isActive());
    }

    public static CourseResponse toResponse(Course c) {
        return new CourseResponse(c.getId(), c.getCode(), c.getTitle(), c.getCredits(),
                c.getSemesterNum(), c.getDepartment().getId(), c.getDepartment().getCode());
    }

    public static AssignmentResponse toResponse(CourseAssignment a) {
        return new AssignmentResponse(a.getId(), a.getCourse().getId(), a.getCourse().getCode(),
                a.getCourse().getTitle(), a.getSemester().getId(),
                a.getSemester().getName() + " " + a.getSemester().getAcademicYear(),
                a.getFaculty().getId(), a.getFaculty().getFullName(), a.getSection());
    }

    public static TimetableSlotResponse toResponse(TimetableSlot t) {
        return new TimetableSlotResponse(t.getId(), t.getAssignment().getId(),
                t.getAssignment().getCourse().getCode(), t.getAssignment().getCourse().getTitle(),
                t.getAssignment().getFaculty().getFullName(), t.getAssignment().getSection(),
                t.getBatch().getId(), t.getBatch().getName(),
                t.getDayOfWeek(), t.getStartTime(), t.getEndTime(), t.getRoom());
    }
}
