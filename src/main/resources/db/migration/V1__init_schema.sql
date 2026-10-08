-- CampusOS initial schema (MySQL 8)

CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','FACULTY','STUDENT') NOT NULL,
  full_name VARCHAR(255) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

CREATE TABLE refresh_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  revoked BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_refresh_token (token),
  KEY idx_refresh_user (user_id),
  CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE password_reset_tokens (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  token VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_prt_token (token),
  KEY idx_prt_user (user_id),
  CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE departments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_departments_code (code)
) ENGINE=InnoDB;

CREATE TABLE semesters (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(50) NOT NULL,
  academic_year VARCHAR(20) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uk_semesters (name, academic_year)
) ENGINE=InnoDB;

CREATE TABLE batches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  department_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  year INT NOT NULL,
  KEY idx_batches_dept (department_id),
  CONSTRAINT fk_batches_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;

CREATE TABLE students (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  roll_number VARCHAR(50) NOT NULL,
  name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NOT NULL,
  phone VARCHAR(20) NULL,
  gender ENUM('MALE','FEMALE','OTHER') NULL,
  dob DATE NULL,
  admission_date DATE NOT NULL,
  guardian_name VARCHAR(255) NULL,
  address VARCHAR(500) NULL,
  status ENUM('ACTIVE','GRADUATED','SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
  department_id BIGINT NULL,
  batch_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_students_roll (roll_number),
  UNIQUE KEY uk_students_user (user_id),
  KEY idx_students_dept (department_id),
  KEY idx_students_batch (batch_id),
  KEY idx_students_name (name),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_students_dept FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT fk_students_batch FOREIGN KEY (batch_id) REFERENCES batches(id)
) ENGINE=InnoDB;

CREATE TABLE courses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  credits INT NOT NULL,
  semester_num INT NOT NULL,
  department_id BIGINT NOT NULL,
  UNIQUE KEY uk_courses_code (code),
  KEY idx_courses_dept (department_id),
  CONSTRAINT fk_courses_dept FOREIGN KEY (department_id) REFERENCES departments(id)
) ENGINE=InnoDB;

CREATE TABLE course_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  semester_id BIGINT NOT NULL,
  faculty_id BIGINT NOT NULL,
  section VARCHAR(10) NOT NULL DEFAULT 'A',
  UNIQUE KEY uk_course_assignment (course_id, semester_id, section),
  KEY idx_ca_faculty (faculty_id),
  KEY idx_ca_semester (semester_id),
  CONSTRAINT fk_ca_course FOREIGN KEY (course_id) REFERENCES courses(id),
  CONSTRAINT fk_ca_semester FOREIGN KEY (semester_id) REFERENCES semesters(id),
  CONSTRAINT fk_ca_faculty FOREIGN KEY (faculty_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE timetable_slots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  day_of_week TINYINT NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  room VARCHAR(50) NOT NULL,
  KEY idx_slots_batch (batch_id, day_of_week),
  KEY idx_slots_assignment (assignment_id),
  CONSTRAINT fk_slots_assignment FOREIGN KEY (assignment_id) REFERENCES course_assignments(id) ON DELETE CASCADE,
  CONSTRAINT fk_slots_batch FOREIGN KEY (batch_id) REFERENCES batches(id)
) ENGINE=InnoDB;

CREATE TABLE attendance_sessions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  batch_id BIGINT NOT NULL,
  class_date DATE NOT NULL,
  taken_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_att_sessions_date (class_date),
  KEY idx_att_sessions_assignment (assignment_id, class_date),
  CONSTRAINT fk_att_sessions_assignment FOREIGN KEY (assignment_id) REFERENCES course_assignments(id),
  CONSTRAINT fk_att_sessions_batch FOREIGN KEY (batch_id) REFERENCES batches(id),
  CONSTRAINT fk_att_sessions_user FOREIGN KEY (taken_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE attendance_records (
  id BIGINT AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  class_date DATE NOT NULL,
  status ENUM('PRESENT','ABSENT','LATE') NOT NULL,
  marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id, class_date),
  KEY idx_att_records_student (student_id, class_date),
  KEY idx_att_records_session (session_id),
  CONSTRAINT fk_att_records_session FOREIGN KEY (session_id) REFERENCES attendance_sessions(id) ON DELETE CASCADE,
  CONSTRAINT fk_att_records_student FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS(class_date) (
  PARTITION p2024 VALUES LESS THAN ('2025-01-01'),
  PARTITION p2025 VALUES LESS THAN ('2026-01-01'),
  PARTITION p2026 VALUES LESS THAN ('2027-01-01'),
  PARTITION p2027 VALUES LESS THAN ('2028-01-01'),
  PARTITION pmax VALUES LESS THAN (MAXVALUE)
);

CREATE TABLE exam_schedules (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  exam_type ENUM('MIDTERM','FINAL','QUIZ','ASSIGNMENT') NOT NULL,
  exam_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  room VARCHAR(50) NOT NULL,
  max_marks INT NOT NULL DEFAULT 100,
  UNIQUE KEY uk_exam_schedule (assignment_id, exam_type),
  KEY idx_exams_date (exam_date),
  CONSTRAINT fk_exams_assignment FOREIGN KEY (assignment_id) REFERENCES course_assignments(id)
) ENGINE=InnoDB;

CREATE TABLE exam_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_schedule_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  marks_obtained DECIMAL(6,2) NOT NULL,
  grade VARCHAR(4) NULL,
  grade_points DECIMAL(3,2) NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_results (exam_schedule_id, student_id),
  KEY idx_results_student (student_id),
  CONSTRAINT fk_results_exam FOREIGN KEY (exam_schedule_id) REFERENCES exam_schedules(id) ON DELETE CASCADE,
  CONSTRAINT fk_results_student FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB;

CREATE TABLE fee_structures (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  semester_id BIGINT NOT NULL,
  department_id BIGINT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  components_json TEXT NULL,
  UNIQUE KEY uk_fee_structure (name, semester_id),
  CONSTRAINT fk_fee_structure_semester FOREIGN KEY (semester_id) REFERENCES semesters(id)
) ENGINE=InnoDB;

CREATE TABLE student_fees (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  fee_structure_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  due_date DATE NOT NULL,
  status ENUM('PENDING','PAID','PARTIAL') NOT NULL DEFAULT 'PENDING',
  paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  KEY idx_student_fees_student (student_id),
  KEY idx_student_fees_status (status),
  CONSTRAINT fk_student_fees_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
  CONSTRAINT fk_student_fees_structure FOREIGN KEY (fee_structure_id) REFERENCES fee_structures(id)
) ENGINE=InnoDB;

CREATE TABLE payments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_fee_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  receipt_no VARCHAR(50) NOT NULL,
  razorpay_order_id VARCHAR(100) NULL,
  razorpay_payment_id VARCHAR(100) NULL,
  razorpay_signature VARCHAR(255) NULL,
  status ENUM('CREATED','PAID','FAILED') NOT NULL DEFAULT 'CREATED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL,
  UNIQUE KEY uk_payments_receipt (receipt_no),
  UNIQUE KEY uk_payments_order (razorpay_order_id),
  KEY idx_payments_student (student_id),
  CONSTRAINT fk_payments_student_fee FOREIGN KEY (student_fee_id) REFERENCES student_fees(id),
  CONSTRAINT fk_payments_student FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB;

CREATE TABLE notices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  body TEXT NOT NULL,
  audience ENUM('ALL','ROLE','DEPARTMENT','BATCH') NOT NULL DEFAULT 'ALL',
  target_role ENUM('ADMIN','FACULTY','STUDENT') NULL,
  department_id BIGINT NULL,
  batch_id BIGINT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notices_created (created_at),
  KEY idx_notices_dept (department_id),
  KEY idx_notices_batch (batch_id),
  CONSTRAINT fk_notices_dept FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT fk_notices_batch FOREIGN KEY (batch_id) REFERENCES batches(id),
  CONSTRAINT fk_notices_user FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE marksheet_jobs (
  id VARCHAR(36) PRIMARY KEY,
  student_id BIGINT NOT NULL,
  status ENUM('PENDING','COMPLETED','FAILED') NOT NULL DEFAULT 'PENDING',
  file_path VARCHAR(500) NULL,
  error VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_marksheet_student (student_id),
  CONSTRAINT fk_marksheet_student FOREIGN KEY (student_id) REFERENCES students(id)
) ENGINE=InnoDB;
