package com.campusos.student;

import com.campusos.academics.Batch;
import com.campusos.academics.BatchRepository;
import com.campusos.academics.Department;
import com.campusos.academics.DepartmentRepository;
import com.campusos.common.ApiException;
import com.campusos.security.dto.AuthDtos.TokenResponse;
import com.campusos.student.dto.StudentDtos.*;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final BatchRepository batchRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<Student> search(String query, Long departmentId, Long batchId, Student.Status status,
                                int page, int size) {
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return studentRepository.search(q, departmentId, batchId, status,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                        Sort.by(Sort.Direction.ASC, "rollNumber")));
    }

    @Transactional(readOnly = true)
    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Student not found: " + id));
    }

    @Transactional(readOnly = true)
    public Student getByUserId(Long userId) {
        return studentRepository.findByUserId(userId)
                .orElseThrow(() -> ApiException.notFound("No student profile for this user"));
    }

    @Transactional
    public StudentResponse admit(CreateStudentRequest req) {
        if (studentRepository.existsByRollNumberIgnoreCase(req.rollNumber())) {
            throw ApiException.conflict("Roll number already exists: " + req.rollNumber());
        }
        if (userRepository.existsByEmailIgnoreCase(req.email())) {
            throw ApiException.conflict("A user with this email already exists");
        }
        User user = User.builder()
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(defaultPassword(req)))
                .role(Role.STUDENT)
                .fullName(req.name())
                .active(true)
                .build();
        userRepository.save(user);

        Student student = Student.builder()
                .user(user)
                .rollNumber(req.rollNumber())
                .name(req.name())
                .email(user.getEmail())
                .phone(req.phone())
                .gender(req.gender())
                .dob(req.dob())
                .admissionDate(req.admissionDate())
                .guardianName(req.guardianName())
                .address(req.address())
                .status(Student.Status.ACTIVE)
                .department(req.departmentId() != null ? departmentRepository.getReferenceById(req.departmentId()) : null)
                .batch(req.batchId() != null ? batchRepository.getReferenceById(req.batchId()) : null)
                .build();
        studentRepository.save(student);
        return StudentMapper.toResponse(student);
    }

    @Transactional
    public StudentResponse update(Long id, UpdateStudentRequest req) {
        Student s = getById(id);
        if (req.name() != null) {
            s.setName(req.name());
            s.getUser().setFullName(req.name());
        }
        if (req.phone() != null) s.setPhone(req.phone());
        if (req.gender() != null) s.setGender(req.gender());
        if (req.dob() != null) s.setDob(req.dob());
        if (req.guardianName() != null) s.setGuardianName(req.guardianName());
        if (req.address() != null) s.setAddress(req.address());
        if (req.status() != null) {
            s.setStatus(req.status());
            s.getUser().setActive(req.status() == Student.Status.ACTIVE);
        }
        if (req.departmentId() != null) {
            Department d = departmentRepository.findById(req.departmentId())
                    .orElseThrow(() -> ApiException.badRequest("Unknown department: " + req.departmentId()));
            s.setDepartment(d);
        }
        if (req.batchId() != null) {
            Batch b = batchRepository.findById(req.batchId())
                    .orElseThrow(() -> ApiException.badRequest("Unknown batch: " + req.batchId()));
            s.setBatch(b);
        }
        return StudentMapper.toResponse(s);
    }

    @Transactional
    public void delete(Long id) {
        Student s = getById(id);
        userRepository.delete(s.getUser());
    }

    /**
     * Bulk CSV import. Header: roll_number,name,email,phone,gender,dob,admission_date,
     * department_code,batch_name,guardian_name,address. Rows with errors are skipped and reported.
     */
    @Transactional
    public CsvImportResult importCsv(byte[] content) {
        List<String> errors = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        Map<String, Department> deptCache = new HashMap<>();
        Map<String, Batch> batchCache = new HashMap<>();
        List<Runnable> deferred = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(new java.io.ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw ApiException.badRequest("Empty CSV file");
            String[] header = parseCsvLine(headerLine);
            Map<String, Integer> col = new HashMap<>();
            for (int i = 0; i < header.length; i++) col.put(header[i].trim().toLowerCase(), i);

            int lineNo = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                String[] f = parseCsvLine(line);
                try {
                    String roll = value(f, col, "roll_number");
                    String name = value(f, col, "name");
                    String email = value(f, col, "email");
                    if (roll.isBlank() || name.isBlank() || email.isBlank()) {
                        throw ApiException.validation("roll_number, name and email are required");
                    }
                    if (studentRepository.existsByRollNumberIgnoreCase(roll)
                            || userRepository.existsByEmailIgnoreCase(email)) {
                        skipped++;
                        errors.add("Line " + lineNo + ": duplicate roll number or email: " + roll);
                        continue;
                    }
                    String deptCode = value(f, col, "department_code");
                    String batchName = value(f, col, "batch_name");
                    Department dept = null;
                    Batch batch = null;
                    if (!deptCode.isBlank()) {
                        dept = deptCache.computeIfAbsent(deptCode.toUpperCase(),
                                c -> departmentRepository.findByCodeIgnoreCase(c)
                                        .orElseThrow(() -> ApiException.badRequest("Unknown department code: " + c)));
                    }
                    if (!batchName.isBlank()) {
                        batch = batchCache.computeIfAbsent(batchName.toUpperCase(),
                                n -> batchRepository.findByNameIgnoreCase(n)
                                        .orElseThrow(() -> ApiException.badRequest("Unknown batch: " + n)));
                    }
                    final Department fDept = dept;
                    final Batch fBatch = batch;
                    User user = User.builder()
                            .email(email.toLowerCase())
                            .passwordHash(passwordEncoder.encode("Student@" + LocalDate.now().getYear()))
                            .role(Role.STUDENT)
                            .fullName(name)
                            .active(true)
                            .build();
                    userRepository.save(user);
                    Student student = Student.builder()
                            .user(user)
                            .rollNumber(roll)
                            .name(name)
                            .email(user.getEmail())
                            .phone(value(f, col, "phone"))
                            .gender(parseGender(value(f, col, "gender")))
                            .dob(parseDate(value(f, col, "dob")))
                            .admissionDate(parseDateOr(value(f, col, "admission_date"), LocalDate.now()))
                            .guardianName(value(f, col, "guardian_name"))
                            .address(value(f, col, "address"))
                            .status(Student.Status.ACTIVE)
                            .department(fDept)
                            .batch(fBatch)
                            .build();
                    studentRepository.save(student);
                    imported++;
                } catch (Exception ex) {
                    skipped++;
                    errors.add("Line " + lineNo + ": " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            throw ApiException.badRequest("Could not read CSV file: " + ex.getMessage());
        }
        deferred.forEach(Runnable::run);
        log.info("CSV import finished: imported={}, skipped={}", imported, skipped);
        return new CsvImportResult(imported, skipped, errors.stream().limit(50).toList());
    }

    private String value(String[] fields, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        if (idx == null || idx >= fields.length) return "";
        return fields[idx] == null ? "" : fields[idx].trim();
    }

    private Student.Gender parseGender(String v) {
        if (v == null || v.isBlank()) return null;
        return switch (v.toUpperCase().charAt(0)) {
            case 'M' -> Student.Gender.MALE;
            case 'F' -> Student.Gender.FEMALE;
            default -> Student.Gender.OTHER;
        };
    }

    private LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return LocalDate.parse(v.trim());
        } catch (DateTimeParseException ex) {
            throw ApiException.validation("Invalid date (expected yyyy-MM-dd): " + v);
        }
    }

    private LocalDate parseDateOr(String v, LocalDate fallback) {
        try {
            return parseDate(v);
        } catch (Exception ex) {
            return fallback;
        }
    }

    /** Splits a CSV line honoring quoted values. */
    static String[] parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }

    private String defaultPassword(CreateStudentRequest req) {
        return "Student@" + LocalDate.now().getYear();
    }
}
