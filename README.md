# CampusOS Backend 🎓

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

CampusOS is an enterprise-grade University ERP and Academic Operating System backend engineered with Spring Boot 3.3.5, Java 21, and MySQL 8. Built to sustain 10,000+ concurrent students, it features partitioned high-throughput attendance logging, automated timetable clash prevention, async marksheet generation, and payment gateway integration.

---

## 🛠️ Architecture & Tech Stack

- **Core Framework**: Spring Boot 3.3.5 (Spring Framework 6, Java 21 LTS)
- **Data Persistence**: Spring Data JPA / Hibernate 6 with HikariCP connection pooling
- **Database**: MySQL 8.0 with **RANGE COLUMNS Partitioning** on attendance records
- **Migrations**: Flyway database version control
- **Security**: Spring Security 6 with stateless JWT (Access + Refresh token rotation), BCrypt password hashing, and role-based access control (`ROLE_ADMIN`, `ROLE_FACULTY`, `ROLE_STUDENT`)
- **Rate Limiting**: Custom token-bucket filter (HTTP 429 after 20 auth req/min)
- **Real-Time Communication**: Spring WebSocket with STOMP message broker (`/ws/campusos`, `/topic/notices/*`)
- **Document Engine**: OpenPDF / iText for async server-side marksheet generation
- **Testing**: JUnit 5, AssertJ, MockMvc, H2 in-memory test database

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK)**: Java 21 or higher
- **Maven**: Version 3.9+
- **MySQL Server**: MySQL 8.0+ running on port `3306`

### Environment Variables & Configuration

The application reads configuration from `src/main/resources/application.yml`. You can override defaults using environment variables or a local configuration profile:

| Variable | Description | Default Value |
| :--- | :--- | :--- |
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/campusos?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` |
| `SPRING_DATASOURCE_USERNAME` | MySQL Username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | MySQL Password | `campusos` |
| `JWT_SECRET` | Secret key for HS256 JWT signing | *(Pre-configured 256-bit secret)* |
| `JWT_EXPIRATION_MS` | Access token lifespan in ms | `86400000` (24 hours) |
| `RAZORPAY_KEY_ID` | Razorpay sandbox API key ID | `rzp_test_campusos` |
| `RAZORPAY_KEY_SECRET` | Razorpay sandbox API secret | `mock_secret_key` |

### Database Setup

1. Start your local MySQL 8 instance:
   ```bash
   mysql -u root -p
   CREATE DATABASE campusos;
   ```
2. When the backend starts, Flyway runs the database migrations (`V1__init_schema.sql` through `V4__create_marksheets_table.sql`) automatically.
3. On initial startup, `DataSeeder` seeds 10,000 students, 12 faculty members, 5 academic departments, 39,000 attendance records, fee dues, and sample notices.

### Running the Application

```bash
# Build and run with Maven
mvn spring-boot:run

# Or run the packaged JAR
mvn clean package -DskipTests
java -jar target/campusos-backend-0.0.1-SNAPSHOT.jar
```

The application will bind to `http://localhost:8080`.

---

## 🧪 Testing

CampusOS includes a complete suite of automated integration tests using an isolated in-memory H2 database profile (`application-test.yml`).

```bash
# Run all automated integration tests
mvn test
```

For complete test logs and E2E verification results, refer to [TEST_REPORT.md](TEST_REPORT.md).

---

## 🔑 Demo Credentials

| Role | Email | Password | Pre-seeded Permissions |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin@campusos.edu` | `Admin@123` | Full administrative control, timetable scheduling, fees management, notices |
| **Faculty Member** | `faculty1@campusos.edu` | `Faculty@123` | Attendance marking, marks entry, timetable viewing |
| **Student** | `student00001@campusos.edu` | `Student@123` | View attendance, timetable, fee dues, pay online, download marksheets |

---

## 📡 API Reference

All REST endpoints are prefixed with `/api/v1`. Secured endpoints require `Authorization: Bearer <accessToken>`.

### Authentication & User Management
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user and receive access + refresh token |
| `POST` | `/api/v1/auth/refresh` | Public | Refresh expired access token using refresh token |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieve profile and role of authenticated user |
| `POST` | `/api/v1/auth/password-reset/request` | Public | Request password reset token via email |
| `POST` | `/api/v1/auth/password-reset/confirm` | Public | Reset password using reset token |

### Academics & Timetable
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/academics/departments` | Authenticated | List all academic departments |
| `GET` | `/api/v1/academics/courses` | Authenticated | List/search courses with pagination |
| `POST` | `/api/v1/academics/courses` | `ROLE_ADMIN` | Create course with credit weightings |
| `GET` | `/api/v1/academics/classrooms` | Authenticated | List available campus lecture halls and labs |
| `POST` | `/api/v1/academics/timetable/slots` | `ROLE_ADMIN` | Schedule class slot (includes automatic clash detection) |
| `GET` | `/api/v1/academics/timetable/section/{id}` | Authenticated | Fetch section timetable |

### Attendance Management
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/attendance/sessions` | `ROLE_FACULTY`, `ROLE_ADMIN` | Record batch attendance session |
| `GET` | `/api/v1/attendance/me` | `ROLE_STUDENT` | Retrieve current student attendance & subject breakdown |
| `GET` | `/api/v1/attendance/shortage` | `ROLE_ADMIN`, `ROLE_FACULTY` | List students with attendance below threshold (default < 75%) |

### Examinations & Marksheets
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/exams` | Authenticated | List upcoming and past examinations |
| `POST` | `/api/v1/exams` | `ROLE_ADMIN` | Schedule examination |
| `POST` | `/api/v1/exams/marks` | `ROLE_FACULTY`, `ROLE_ADMIN` | Record marks for students with max-marks validation |
| `POST` | `/api/v1/exams/marksheets/generate` | `ROLE_ADMIN` | Asynchronously generate student PDF marksheet |
| `GET` | `/api/v1/exams/marksheets/student/{id}/download` | Authenticated | Download student marksheet PDF |

### Fees & Payments
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/fees/structures` | `ROLE_ADMIN` | View created fee structures |
| `POST` | `/api/v1/fees/structures` | `ROLE_ADMIN` | Create fee structure |
| `POST` | `/api/v1/fees/assign` | `ROLE_ADMIN` | Batch assign fee structure to students |
| `POST` | `/api/v1/fees/orders` | `ROLE_STUDENT` | Generate Razorpay payment order for outstanding dues |
| `POST` | `/api/v1/fees/verify` | `ROLE_STUDENT` | Verify Razorpay payment signature and issue receipt |
| `GET` | `/api/v1/fees/my` | `ROLE_STUDENT` | View outstanding and paid fees for current student |

### Notice Board & WebSockets
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/notices` | Authenticated | List notices filtered by audience (`ALL`, `STUDENT`, `FACULTY`) |
| `POST` | `/api/v1/notices` | `ROLE_ADMIN` | Publish notice and trigger real-time STOMP broadcast |
| `WS` | `/ws/campusos` | Public / Auth | STOMP connection endpoint (subscribes to `/topic/notices/*`) |

### Role Dashboards
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/dashboard/admin` | `ROLE_ADMIN` | Admin KPI metrics, 14-day attendance trend, pending fee totals |
| `GET` | `/api/v1/dashboard/faculty` | `ROLE_FACULTY` | Faculty daily schedule, active course list |
| `GET` | `/api/v1/dashboard/student` | `ROLE_STUDENT` | Student registered courses, overall attendance %, pending fees |

---

## ⚡ High-Throughput Range Partitioning

The `attendance_records` table is partitioned using MySQL 8 range columns on `class_date`:
```sql
CREATE TABLE attendance_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    class_date DATE NOT NULL,
    status ENUM('PRESENT', 'ABSENT', 'LATE', 'EXCUSED') NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, class_date),
    KEY idx_student_date (student_id, class_date)
) ENGINE=InnoDB
PARTITION BY RANGE COLUMNS(class_date) (
    PARTITION p2026_h1 VALUES LESS THAN ('2026-07-01'),
    PARTITION p2026_h2 VALUES LESS THAN ('2027-01-01'),
    PARTITION p2027_h1 VALUES LESS THAN ('2027-07-01'),
    PARTITION p_max VALUES LESS THAN MAXVALUE
);
```
This guarantees sub-millisecond query latency for historical reports and eliminates lock contention during bulk morning attendance logging.
