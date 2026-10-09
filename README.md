# CampusOS Backend

Backend REST API for the CampusOS college management system, built using Java, Spring Boot 3, and MySQL.

---

## Overview

CampusOS Backend provides the server-side logic and database operations for managing everyday college tasks. It supports role-based access control for Administrators, Faculty members, and Students, handling:
- User authentication and role management (JWT tokens)
- Department and course curriculum management
- Weekly timetable scheduling with conflict validation
- Daily lecture attendance recording and shortage tracking (<75%)
- Exam schedules, marks entry, and PDF marksheet downloads
- Student fee structure tracking and payment verification
- College notice board broadcasts

---

## Tech Stack

- **Language**: Java 21 LTS
- **Framework**: Spring Boot 3.3.5
- **Security**: Spring Security with JWT (Access and Refresh tokens)
- **Database**: MySQL 8.0
- **ORM & Data Access**: Spring Data JPA / Hibernate
- **Database Migrations**: Flyway
- **Real-Time Messaging**: Spring WebSocket with STOMP over SockJS
- **PDF Generation**: iText for generating downloadable marksheets
- **Build Tool**: Apache Maven (includes Maven Wrapper `mvnw`)

---

## Getting Started

### Prerequisites

- Java 17 or Java 21 JDK installed
- MySQL 8.0 running locally on port `3306`

### Database Setup

Create the MySQL database:
```sql
CREATE DATABASE campusos;
```
Default connection settings in `application.yml`:
- Host: `localhost:3306`
- Username: `root`
- Password: `campusos`
- Database: `campusos`

### Running the Backend

You can run the application using the included Maven Wrapper:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

The REST API will be available at: `http://localhost:8080/api/v1`  
Health check endpoint: `http://localhost:8080/actuator/health`

---

## Core API Endpoints

- `POST /api/v1/auth/login`: Authenticate user and receive JWT token
- `GET /api/v1/auth/me`: Get current logged-in user profile
- `GET /api/v1/dashboard/admin`: Get admin summary statistics
- `GET /api/v1/departments`: List college departments
- `GET /api/v1/courses`: List semester courses
- `POST /api/v1/attendance/sessions`: Record attendance for a class session
- `GET /api/v1/attendance/my`: View student's attendance records
- `GET /api/v1/fees/my`: View student's fee dues and payment history
- `POST /api/v1/notices`: Post an official college notice
- `GET /api/v1/exams/{id}/marksheet/pdf`: Download semester marksheet as PDF

---

## Demo Credentials

- **Admin**: `admin@campusos.edu` (Password: `Admin@123`)
- **Faculty**: `faculty1@campusos.edu` (Password: `Faculty@123`)
- **Student**: `student00001@campusos.edu` (Password: `Student@123`)
