package com.campusos.attendance;

import com.campusos.academics.BatchRepository;
import com.campusos.academics.CourseAssignment;
import com.campusos.academics.CourseAssignmentRepository;
import com.campusos.attendance.dto.AttendanceDtos.*;
import com.campusos.attendance.dto.AttendanceDtos.CourseAttendanceResponse;
import com.campusos.common.ApiException;
import com.campusos.security.AuthPrincipal;
import com.campusos.student.Student;
import com.campusos.student.StudentRepository;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    public static final double MINIMUM_ATTENDANCE_PERCENT = 75.0;

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final BatchRepository batchRepository;

    /** Faculty marks one class: creates the session and all student records in a single transaction. */
    @Transactional
    public SessionResponse markSession(AuthPrincipal principal, MarkSessionRequest req) {
        if (req.classDate().isAfter(LocalDate.now().plusDays(1))) {
            throw ApiException.badRequest("Cannot mark attendance for a future date");
        }
        CourseAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> ApiException.badRequest("Unknown course assignment"));
        if ("FACULTY".equals(principal.role()) && !assignment.getFaculty().getId().equals(principal.id())) {
            throw ApiException.forbidden("You can only mark attendance for your own classes");
        }
        if (sessionRepository.existsByAssignmentIdAndClassDate(req.assignmentId(), req.classDate())) {
            throw ApiException.conflict("Attendance already marked for this class on " + req.classDate());
        }
        // Pre-fetch all students of the batch in one query; validate requested ids against it.
        List<Student> batchStudents = studentRepository
                .search(null, null, req.batchId(), null, org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent();
        Map<Long, Student> byId = new HashMap<>();
        batchStudents.forEach(s -> byId.put(s.getId(), s));

        var session = sessionRepository.save(AttendanceSession.builder()
                .assignment(assignment)
                .batch(batchRepository.getReferenceById(req.batchId()))
                .classDate(req.classDate())
                .takenBy(userRepository.getReferenceById(principal.id()))
                .createdAt(Instant.now())
                .build());

        List<AttendanceRecord> records = new ArrayList<>(req.records().size());
        for (MarkRecord mr : req.records()) {
            Student student = byId.get(mr.studentId());
            if (student == null) {
                throw ApiException.badRequest("Student " + mr.studentId() + " is not in this batch");
            }
            records.add(AttendanceRecord.builder()
                    .session(session)
                    .classDate(req.classDate())
                    .student(student)
                    .status(mr.status())
                    .markedAt(Instant.now())
                    .build());
        }
        recordRepository.saveAll(records);

        return new SessionResponse(session.getId(), assignment.getId(),
                assignment.getCourse().getCode(), assignment.getCourse().getTitle(),
                req.batchId(), session.getBatch().getName(), req.classDate(),
                assignment.getFaculty().getFullName());
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> sessionsForAssignment(Long assignmentId) {
        return sessionRepository.findByAssignmentIdOrderByClassDateDesc(assignmentId).stream()
                .map(s -> new SessionResponse(s.getId(), s.getAssignment().getId(),
                        s.getAssignment().getCourse().getCode(), s.getAssignment().getCourse().getTitle(),
                        s.getBatch().getId(), s.getBatch().getName(), s.getClassDate(),
                        s.getTakenBy().getFullName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MarkedRecordResponse> sessionRecords(Long sessionId) {
        return recordRepository.findBySessionId(sessionId).stream()
                .map(r -> new MarkedRecordResponse(r.getStudent().getId(),
                        r.getStudent().getRollNumber(), r.getStudent().getName(), r.getStatus()))
                .toList();
    }

    /** Student view: overall percentage, per-course breakdown and shortage alerts below 75%. */
    @Transactional(readOnly = true)
    public StudentAttendanceResponse studentAttendance(Long studentId) {
        OverallAttendanceStat overall = recordRepository.statsOverall(studentId);
        List<CourseAttendanceResponse> perCourse = recordRepository.statsPerCourse(studentId).stream()
                .map(s -> new CourseAttendanceResponse(s.courseId(), s.courseCode(), s.courseTitle(),
                        s.present(), s.total(), s.percentage()))
                .toList();
        List<ShortageAlert> alerts = perCourse.stream()
                .filter(c -> c.total() > 0 && c.percentage() < MINIMUM_ATTENDANCE_PERCENT)
                .map(c -> new ShortageAlert(studentId, null, null, c.courseId(), c.courseCode(), c.percentage()))
                .toList();
        return new StudentAttendanceResponse(overall.percentage(), overall.present() == null ? 0 : overall.present(),
                overall.total() == null ? 0 : overall.total(), perCourse, alerts);
    }

    /** Batch-wide shortage list (students below 75% overall). Single grouped query. */
    @Transactional(readOnly = true)
    public List<ShortageAlert> batchShortage(Long batchId) {
        return recordRepository.statsPerStudentInBatch(batchId).stream()
                .filter(s -> s.percentage() < MINIMUM_ATTENDANCE_PERCENT)
                .map(s -> new ShortageAlert(s.studentId(), s.rollNumber(), s.name(),
                        null, "OVERALL", s.percentage()))
                .toList();
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) throw ApiException.notFound("Session not found");
        sessionRepository.deleteById(sessionId);
    }

    /** Roster of students in a batch for the marking screen. */
    @Transactional(readOnly = true)
    public List<RosterEntry> roster(Long batchId) {
        return studentRepository
                .search(null, null, batchId, Student.Status.ACTIVE, org.springframework.data.domain.PageRequest.of(0, 500))
                .getContent().stream()
                .map(s -> new RosterEntry(s.getId(), s.getRollNumber(), s.getName()))
                .toList();
    }
}
