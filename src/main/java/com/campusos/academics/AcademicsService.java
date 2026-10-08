package com.campusos.academics;

import com.campusos.academics.dto.AcademicsDtos.*;
import com.campusos.common.ApiException;
import com.campusos.user.Role;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AcademicsService {

    private final DepartmentRepository departmentRepository;
    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseAssignmentRepository assignmentRepository;
    private final TimetableSlotRepository slotRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;

    // ---------- Departments ----------
    @Transactional(readOnly = true)
    public List<Department> listDepartments() {
        return departmentRepository.findAll(Sort.by("code"));
    }

    @Transactional
    public Department createDepartment(DepartmentRequest req) {
        if (departmentRepository.existsByCodeIgnoreCase(req.code())) {
            throw ApiException.conflict("Department code already exists: " + req.code());
        }
        return departmentRepository.save(Department.builder().code(req.code().toUpperCase(Locale.ROOT)).name(req.name()).build());
    }

    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) throw ApiException.notFound("Department not found");
        departmentRepository.deleteById(id);
    }

    // ---------- Semesters ----------
    @Transactional(readOnly = true)
    public List<Semester> listSemesters() {
        return semesterRepository.findAll(Sort.by(Sort.Direction.DESC, "startDate"));
    }

    @Transactional
    public Semester createSemester(SemesterRequest req) {
        if (req.startDate().isAfter(req.endDate())) {
            throw ApiException.badRequest("Semester start date must be before end date");
        }
        if (req.active()) {
            semesterRepository.findByActiveTrue().ifPresent(s -> {
                throw ApiException.conflict("Another semester is already active. Deactivate it first.");
            });
        }
        return semesterRepository.save(Semester.builder()
                .name(req.name()).academicYear(req.academicYear())
                .startDate(req.startDate()).endDate(req.endDate())
                .active(req.active()).build());
    }

    @Transactional
    @CacheEvict(cacheNames = "activeSemester", allEntries = true)
    public Semester setActive(Long id, boolean active) {
        Semester s = semesterRepository.findById(id).orElseThrow(() -> ApiException.notFound("Semester not found"));
        if (active) {
            semesterRepository.findByActiveTrue().ifPresent(other -> {
                if (!other.getId().equals(id)) throw ApiException.conflict("Another semester is already active");
            });
        }
        s.setActive(active);
        return semesterRepository.save(s);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "activeSemester")
    public Semester getActiveSemester() {
        return semesterRepository.findByActiveTrue()
                .orElseThrow(() -> ApiException.badRequest("No active semester configured"));
    }

    // ---------- Courses ----------
    @Transactional(readOnly = true)
    public Page<Course> listCourses(Long departmentId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by("code"));
        return departmentId == null ? courseRepository.findAll(pr) : courseRepository.findByDepartmentId(departmentId, pr);
    }

    @Transactional
    public Course createCourse(CourseRequest req) {
        if (courseRepository.existsByCodeIgnoreCase(req.code())) {
            throw ApiException.conflict("Course code already exists: " + req.code());
        }
        Department dept = departmentRepository.findById(req.departmentId())
                .orElseThrow(() -> ApiException.badRequest("Unknown department: " + req.departmentId()));
        return courseRepository.save(Course.builder()
                .code(req.code().toUpperCase(Locale.ROOT)).title(req.title())
                .credits(req.credits()).semesterNum(req.semesterNum()).department(dept).build());
    }

    @Transactional
    public void deleteCourse(Long id) {
        if (!courseRepository.existsById(id)) throw ApiException.notFound("Course not found");
        courseRepository.deleteById(id);
    }

    // ---------- Faculty assignment ----------
    @Transactional(readOnly = true)
    public List<CourseAssignment> listAssignments(Long semesterId) {
        return assignmentRepository.findBySemesterId(semesterId);
    }

    @Transactional
    public CourseAssignment assignFaculty(AssignmentRequest req) {
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> ApiException.badRequest("Unknown course"));
        Semester semester = semesterRepository.findById(req.semesterId())
                .orElseThrow(() -> ApiException.badRequest("Unknown semester"));
        var faculty = userRepository.findById(req.facultyId())
                .filter(u -> u.getRole() == Role.FACULTY)
                .orElseThrow(() -> ApiException.badRequest("Faculty user not found"));
        String section = req.section() == null ? "A" : req.section().toUpperCase(Locale.ROOT);
        if (assignmentRepository.existsByCourseIdAndSemesterIdAndSection(req.courseId(), req.semesterId(), section)) {
            throw ApiException.conflict("This course is already assigned for the semester/section");
        }
        return assignmentRepository.save(CourseAssignment.builder()
                .course(course).semester(semester).faculty(faculty).section(section).build());
    }

    @Transactional
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) throw ApiException.notFound("Assignment not found");
        assignmentRepository.deleteById(id);
    }

    // ---------- Timetable ----------
    @Transactional(readOnly = true)
    public List<TimetableSlot> timetableForBatch(Long batchId) {
        return slotRepository.findByBatch(batchId);
    }

    @Transactional(readOnly = true)
    public List<TimetableSlot> timetableForFaculty(Long facultyId) {
        return slotRepository.findByFaculty(facultyId);
    }

    /**
     * Creates a timetable slot with clash detection across three axes:
     * batch overlap, faculty overlap and room overlap (same day, overlapping time).
     */
    @Transactional
    public TimetableSlot createSlot(TimetableSlotRequest req) {
        if (!req.startTime().isBefore(req.endTime())) {
            throw ApiException.badRequest("Slot start time must be before end time");
        }
        CourseAssignment assignment = assignmentRepository.findById(req.assignmentId())
                .orElseThrow(() -> ApiException.badRequest("Unknown course assignment"));
        Batch batch = batchRepository.findById(req.batchId())
                .orElseThrow(() -> ApiException.badRequest("Unknown batch"));

        List<TimetableSlot> clashes = slotRepository.findClashes(
                req.batchId(), assignment.getFaculty().getId(), req.room(),
                req.dayOfWeek(), req.startTime(), req.endTime());

        for (TimetableSlot clash : clashes) {
            LocalTime cs = clash.getStartTime(), ce = clash.getEndTime();
            boolean overlaps = cs.isBefore(req.endTime()) && ce.isAfter(req.startTime());
            if (!overlaps) continue;
            if (clash.getBatch().getId().equals(req.batchId())) {
                throw ApiException.conflict("TIMETABLE_CLASH: Batch already has a class at this time ("
                        + clash.getAssignment().getCourse().getCode() + ", " + cs + "-" + ce + ")");
            }
            if (clash.getAssignment().getFaculty().getId().equals(assignment.getFaculty().getId())) {
                throw ApiException.conflict("TIMETABLE_CLASH: Faculty already teaches at this time ("
                        + clash.getAssignment().getCourse().getCode() + ", " + cs + "-" + ce + ")");
            }
            if (clash.getRoom().equalsIgnoreCase(req.room())) {
                throw ApiException.conflict("TIMETABLE_CLASH: Room " + req.room() + " is occupied at this time ("
                        + clash.getAssignment().getCourse().getCode() + ", " + cs + "-" + ce + ")");
            }
        }
        return slotRepository.save(TimetableSlot.builder()
                .assignment(assignment).batch(batch)
                .dayOfWeek(req.dayOfWeek())
                .startTime(req.startTime()).endTime(req.endTime())
                .room(req.room()).build());
    }

    @Transactional
    public void deleteSlot(Long id) {
        if (!slotRepository.existsById(id)) throw ApiException.notFound("Slot not found");
        slotRepository.deleteById(id);
    }
}
