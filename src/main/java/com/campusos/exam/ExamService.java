package com.campusos.exam;

import com.campusos.academics.CourseAssignment;
import com.campusos.academics.CourseAssignmentRepository;
import com.campusos.common.ApiException;
import com.campusos.exam.dto.ExamDtos.*;
import com.campusos.exam.dto.ExamDtos.ResultRow;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamScheduleRepository scheduleRepository;
    private final ExamResultRepository resultRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final MarksheetJobRepository jobRepository;
    private final GradeService gradeService;
    private final MarksheetPdfService pdfService;

    // ---------- Schedules ----------
    @Transactional(readOnly = true)
    public List<ScheduleResponse> schedules(Long semesterId) {
        return scheduleRepository.findSchedules(semesterId).stream()
                .map(this::toScheduleResponse).toList();
    }

    @Transactional
    public ScheduleResponse createSchedule(ScheduleRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw ApiException.badRequest("Exam start time must be before end time");
        }
        CourseAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> ApiException.badRequest("Unknown course assignment"));
        if (scheduleRepository.existsByAssignmentIdAndExamType(req.assignmentId(), req.examType())) {
            throw ApiException.conflict("Exam already scheduled for this course: " + req.examType());
        }
        // Clash detection: same date, overlapping time, same room or same batch.
        for (ExamSchedule other : scheduleRepository.findAll()) {
            if (!other.getExamDate().equals(req.examDate())) continue;
            boolean overlaps = other.getStartTime().isBefore(req.endTime())
                    && other.getEndTime().isAfter(req.startTime());
            if (!overlaps) continue;
            if (other.getRoom().equalsIgnoreCase(req.room())
                    && other.getAssignment().getId().equals(req.assignmentId())) {
                throw ApiException.conflict("TIMETABLE_CLASH: course already has an exam in this slot");
            }
        }
        ExamSchedule saved = scheduleRepository.save(ExamSchedule.builder()
                .assignment(assignment).examType(req.examType()).examDate(req.examDate())
                .startTime(req.startTime()).endTime(req.endTime()).room(req.room())
                .maxMarks(req.maxMarks()).build());
        return toScheduleResponseRow(saved);
    }

    private ScheduleResponse toScheduleResponse(ExamScheduleRepository.ScheduleWithCourse s) {
        return new ScheduleResponse(s.getId(), s.getAssignmentId(), s.getCourseCode(), s.getCourseTitle(),
                s.getFacultyName(), s.getSemesterName(), s.getExamType(), s.getExamDate(),
                s.getStartTime(), s.getEndTime(), s.getRoom(), s.getMaxMarks());
    }

    private ScheduleResponse toScheduleResponseRow(ExamSchedule s) {
        return new ScheduleResponse(s.getId(), s.getAssignment().getId(),
                s.getAssignment().getCourse().getCode(), s.getAssignment().getCourse().getTitle(),
                s.getAssignment().getFaculty().getFullName(),
                s.getAssignment().getSemester().getName() + " " + s.getAssignment().getSemester().getAcademicYear(),
                s.getExamType(), s.getExamDate(), s.getStartTime(), s.getEndTime(), s.getRoom(), s.getMaxMarks());
    }

    // ---------- Marks entry ----------
    @Transactional
    public MarksEntryResult enterMarks(AuthPrincipal principal, MarksEntryRequest req) {
        ExamSchedule schedule = scheduleRepository.findById(req.examScheduleId())
                .orElseThrow(() -> ApiException.notFound("Exam schedule not found"));
        if ("FACULTY".equals(principal.role()) && !schedule.getAssignment().getFaculty().getId().equals(principal.id())) {
            throw ApiException.forbidden("You can only enter marks for your own courses");
        }
        int saved = 0, errors = 0;
        List<String> messages = new ArrayList<>();
        for (MarkEntry entry : req.marks()) {
            Student student = studentRepository.findById(entry.studentId()).orElse(null);
            if (student == null) {
                errors++;
                messages.add("Unknown student: " + entry.studentId());
                continue;
            }
            if (entry.marks().compareTo(BigDecimal.ZERO) < 0
                    || entry.marks().compareTo(BigDecimal.valueOf(schedule.getMaxMarks())) > 0) {
                errors++;
                messages.add("Marks out of range for " + student.getRollNumber());
                continue;
            }
            double percent = entry.marks().doubleValue() * 100.0 / schedule.getMaxMarks();
            var grade = gradeService.compute(percent);
            ExamResult result = resultRepository
                    .findByExamScheduleIdAndStudentId(schedule.getId(), entry.studentId())
                    .orElseGet(() -> ExamResult.builder()
                            .examSchedule(schedule).student(student).build());
            result.setMarksObtained(entry.marks());
            result.setGrade(grade.letter());
            result.setGradePoints(gradeService.pointsFor(percent));
            resultRepository.save(result);
            saved++;
        }
        return new MarksEntryResult(saved, errors, messages.stream().limit(50).toList());
    }

    // ---------- Student results ----------
    @Transactional(readOnly = true)
    public StudentResultsResponse studentResults(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        var rawRows = resultRepository.findStudentResults(studentId);
        List<ResultRow> rows = rawRows.stream()
                .map(r -> new ResultRow(r.getCourseId(), r.getCourseCode(), r.getCourseTitle(), r.getCredits(),
                        r.getSemesterId(), r.getSemesterName(), r.getExamType(),
                        r.getMarksObtained(), r.getMaxMarks(), r.getGrade(), r.getGradePoints()))
                .toList();
        double cgpa = gradeService.computeGpa(rawRows, null);
        List<SemesterGpa> sgpas = rawRows.stream()
                .map(ExamResultRepository.StudentResultRow::getSemesterId)
                .distinct()
                .map(sid -> {
                    String name = rawRows.stream().filter(r -> sid.equals(r.getSemesterId()))
                            .findFirst().orElseThrow().getSemesterName();
                    return new SemesterGpa(sid, name, gradeService.computeGpa(rawRows, sid));
                })
                .toList();
        return new StudentResultsResponse(studentId, student.getRollNumber(), student.getName(), cgpa, sgpas, rows);
    }

    // ---------- Marksheet ----------
    public MarksheetJobResponse requestMarksheet(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> ApiException.notFound("Student not found"));
        String jobId = java.util.UUID.randomUUID().toString();
        jobRepository.save(MarksheetJob.builder()
                .id(jobId).student(student).status(MarksheetJob.Status.PENDING)
                .createdAt(java.time.Instant.now()).build());
        pdfService.renderAsync(jobId, studentId);
        return new MarksheetJobResponse(jobId, "PENDING", "/api/v1/exams/marksheets/" + jobId + "/download");
    }

    public MarksheetJob marksheetJob(String jobId) {
        MarksheetJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("Marksheet job not found"));
        return job;
    }
}
