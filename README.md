# University Management System

Harborview University portal for the software lab: a Spring Boot web application with a REST API, three roles, and Docker.

## Run

```bash
docker compose up --build
```

Open http://localhost:8080

API docs: http://localhost:8080/swagger-ui.html

The first build downloads Maven and the base images, so it takes a few minutes. Postgres keeps its data in a volume. To wipe the database and load the demo accounts again:

```bash
docker compose down -v
docker compose up --build
```

## Demo accounts

| Role | Name | Username | Password |
| --- | --- | --- | --- |
| Admin | Alex Morgan | `admin` | `admin123` |
| Teacher | Dr. Sarah Khan | `teacher` | `teacher123` |
| Teacher | Prof. James Okonkwo | `teacher2` | `teacher123` |
| Student | Ayesha Rahman | `student` | `student123` |
| Student | Liam Chen | `student2` | `student123` |
| Student | Maya Iqbal | `student3` | `student123` |

Sarah teaches CS101 and CS201. James teaches MATH101. Ayesha is already enrolled in CS101, and Liam is enrolled in MATH101. Maya has no enrollment, so she is the cleanest account for an enroll demo. BUS110 has no teacher until an admin assigns one.

## What each role can do

| Action | Admin | Teacher | Student |
| --- | --- | --- | --- |
| Sign in and view own profile | yes | yes | yes |
| Manage people | yes | no | no |
| View departments and courses | yes | yes | yes |
| Create, update, and delete departments and courses | yes | no | no |
| Enroll a student | any student | no | only themselves |
| View enrollments | all | own courses | own only |
| Record a grade | any course | own courses | no |
| Drop an ungraded enrollment | any | no | own only |

Deleting a course also removes its enrollments. A department cannot be deleted while it still has courses. A teacher cannot be deleted while they are assigned to a course. Nobody can delete their own account, and the last admin cannot be removed.

## API

Authentication is a JWT from `POST /api/auth/login`. Send it as `Authorization: Bearer <token>`. The API is stateless: there is no server session, and CSRF is disabled because the browser does not send a session cookie.

| Method | Path | Access |
| --- | --- | --- |
| POST | `/api/auth/login` | public |
| GET | `/api/users/me` | signed in |
| GET, POST, PUT, DELETE | `/api/users` | admin |
| GET | `/api/departments` | signed in |
| POST, PUT, DELETE | `/api/departments` | admin |
| GET | `/api/courses` | signed in |
| GET | `/api/courses/teaching` | teacher |
| POST, PUT, DELETE | `/api/courses` | admin |
| GET | `/api/enrollments` | signed in, scoped to the caller |
| POST, DELETE | `/api/enrollments` | admin, or a student acting on their own record |
| PATCH | `/api/enrollments/{id}/grade` | admin, or the teacher of that course |

Grades are `A`, `A-`, `B+`, `B`, `B-`, `C+`, `C`, `C-`, `D`, and `F`. Sending an empty grade clears it.

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Passwords are stored with BCrypt. Authorization is enforced twice: `@PreAuthorize` on the controllers, and ownership checks in the services so a teacher cannot grade another teacher's course even if they know the enrollment id.

## Tests

The suite has **6 unit tests** (mocked services/JWT) and **3 integration tests** (full Spring context + MockMvc).

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9-eclipse-temurin-21 mvn -B test
```

GitHub Actions runs the same `mvn -B test` on every push and pull request to `main` / `master` (see `.github/workflows/ci.yml`).

## Run without Docker

Java 21 is required. With Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses an in-memory H2 database and the same demo accounts. The Docker stack uses PostgreSQL.
