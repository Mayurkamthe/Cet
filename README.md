# CET Mock Test Portal

A complete **Computer Based Test (CBT)** portal for coaching institutes supporting **CET / NEET / JEE** mock exams.

Built with Java 21 · Spring Boot 3 · Spring Security 6 · Thymeleaf · Tailwind CSS · SQLite · Apache PDFBox

---

## Features

### Admin
- Dashboard with stats (students, exams, results, activity)
- Student management — add, edit, delete, reset password, bulk Excel import
- Exam management — create, edit, publish, close exams
- PDF question paper upload with automatic page-to-image conversion (PDFBox)
- Answer key management — enter correct answers per question
- Results — view all student submissions with score breakdown

### Student
- Login and dashboard
- View available (published) exams
- CBT exam interface — question image display, option palette, mark for review, countdown timer
- Auto-save answers on navigation
- Submit exam and view instant result with score, percentage, correct/wrong/unanswered breakdown
- My Results — history of all past attempts

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3 |
| Security | Spring Security 6 |
| ORM | Spring Data JPA + Hibernate |
| Database | SQLite (file-based, zero setup) |
| Templates | Thymeleaf + Thymeleaf Security Extras |
| CSS | Tailwind CSS (CDN) |
| JS | Alpine.js (CDN) |
| PDF Processing | Apache PDFBox 3 |
| Build | Maven |
| Utilities | Lombok |

---

## Project Structure

```
src/main/java/com/aparaitech/cet/
├── config/          # SecurityConfig, WebMvcConfig, DataInitializer, GlobalExceptionHandler
├── controller/      # AuthController, AdminDashboard, AdminStudents, AdminExams, AdminResults, Student
├── entity/          # User, Role, Exam, QuestionPaper, QuestionPage, AnswerKey, ExamAttempt, StudentAnswer, Result
├── repository/      # Spring Data JPA repositories
├── security/        # CustomUserDetailsService
└── service/         # UserService, ExamService, PdfService, AnswerKeyService, ExamAttemptService, ExcelImportService

src/main/resources/
├── templates/
│   ├── layouts/     # main.html (sidebar, header, flash messages)
│   ├── auth/        # login.html
│   ├── admin/       # dashboard, students, exams, results
│   └── student/     # dashboard, exams, exam-take, result, results, profile
├── static/
│   └── uploads/     # PDF files and rendered question page images
└── application.properties
```

---

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.8+

### Run

```bash
git clone https://github.com/Mayurkamthe/Cet.git
cd Cet
mvn spring-boot:run
```

App starts at **http://localhost:8080**

### Default Admin Login

Credentials are set in `application.properties`:

```properties
app.admin.username=admin
app.admin.password=admin123
```

> ⚠️ **Change these before deploying to production.**

The admin user is seeded automatically on first startup if no admin exists.

---

## Configuration

All key settings are in `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# SQLite DB location (relative to working directory)
spring.datasource.url=jdbc:sqlite:./data/cetportal.db

# Admin seed credentials (used only on first run)
app.admin.username=admin
app.admin.password=admin123
app.admin.fullName=Administrator
app.admin.email=admin@cetportal.com

# Upload directories for PDF files and rendered question images
app.upload.dir=./src/main/resources/static/uploads
app.pages.dir=./src/main/resources/static/uploads/pages

# Schema auto-update (use 'validate' in production)
spring.jpa.hibernate.ddl-auto=update

# Thymeleaf cache (set true in production)
spring.thymeleaf.cache=false
```

---

## Roles

| Role | Access |
|---|---|
| `ROLE_ADMIN` | Full admin panel — students, exams, results |
| `ROLE_STUDENT` | Student portal — available exams, CBT interface, results |

No public self-registration. Only admin can create student accounts.

---

## Exam Workflow

```
Admin creates exam (DRAFT)
    → Uploads PDF question paper (auto-split to page images)
    → Enters answer key (A/B/C/D per question)
    → Publishes exam

Student logs in
    → Sees published exam on dashboard
    → Starts exam (timer begins)
    → Navigates questions (answers auto-saved via AJAX)
    → Submits exam

System calculates result
    → Correct × marksPerCorrect − Wrong × negativeMarks
    → Stores result with percentage breakdown
```

---

## Scoring

```
Final Score = (Correct × marksPerCorrect) − (Wrong × negativeMarks)
Percentage  = (Final Score / Max Score) × 100
```

Default: **+4** per correct, **−1** per wrong (configurable per exam).

---

## Production Tips

- Set `spring.jpa.hibernate.ddl-auto=validate` after initial setup
- Set `spring.thymeleaf.cache=true`
- Change `app.admin.password` to a strong password
- Move upload directories to an absolute path outside the project
- Use a reverse proxy (Nginx) in front of the Spring Boot app

---

## Developer

**Mayur Kamthe** — Aparaitech Software, Baramati  
GitHub: [@Mayurkamthe](https://github.com/Mayurkamthe)
