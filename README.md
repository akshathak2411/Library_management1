# Library Book Management and Reading Pattern Analysis System

A full-stack college library project for Scenario 7, with separate Student and Library Admin login portals.

## Technologies
- Frontend: HTML, CSS, JavaScript and Vite (vanilla JavaScript; no React framework)
- Backend: Java 17+, Spring Boot, Spring Data JPA and REST API
- Database: H2 relational database (file-persistent)
- Login: server-side HTTP session; BCrypt password hashes
- Analysis: Python 3, requests, pandas and matplotlib

## Requirements
Install Java JDK 17 or newer, Apache Maven 3.8+, Node.js (npm is included with Node.js) and Python 3.10+.

## Run in VS Code
Open the `LibraryManagementSystem` folder in VS Code. Keep each terminal open.

### Terminal 1 — Java backend
```bash
cd backend
mvn spring-boot:run
```
If Windows does not recognize `mvn`, use the Maven executable path you installed, for example:
```bat
C:\apache-maven-3.10.1\bin\mvn.cmd spring-boot:run
```
Backend: http://localhost:8080

### Terminal 2 — Frontend
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:5173

The Vite development server proxies `/api` requests to `http://localhost:8080`, avoiding browser CORS and cookie issues. If the backend uses another port, set `API_PROXY_TARGET` before starting Vite (PowerShell example: `$env:API_PROXY_TARGET = 'http://localhost:8081'`). Restart both servers after backend/API or Vite proxy changes so each process loads the latest code.

### Terminal 3 — Python analysis (optional)
Start the backend first. The script prompts for the library admin credentials; alternatively set `LIBRARY_ADMIN_USERNAME`, `LIBRARY_ADMIN_PASSWORD`, and optionally `LIBRARY_API_URL` (the default API URL is `http://localhost:8080/api`).
```bash
cd python-analysis
python -m pip install -r requirements.txt
python analyze.py
```
Charts and the cleaned CSV are saved in `python-analysis/output/`.

## Deploy on Render
The repository includes a Render Blueprint (`render.yaml`) and a Dockerfile that builds the Vite frontend into the Spring Boot app. The deployed frontend and API share one HTTPS origin, so login sessions work without a separate frontend/API CORS setup. H2 data is stored on a mounted persistent disk.

1. Push this repository to GitHub and open [Render Blueprints](https://dashboard.render.com/blueprints).
2. Select **New Blueprint Instance**, connect `akshathak2411/Library_management1`, and deploy the `render.yaml` blueprint.
3. When prompted, set strong, unique values for `ADMIN_PASSWORD` and `STUDENT_DEMO_PASSWORD`. These are deployment secrets; do not commit them.
4. Wait for the service health check at `/api/health` to pass, then open the HTTPS URL Render assigns to the service.

This Blueprint uses a paid Render web service and a 1 GB persistent disk; Render's free web services do not provide persistent disks. Keep the disk attached to preserve the H2 database across deployments. Seeded demo students use the `STUDENT_DEMO_PASSWORD` secret; newly registered students use the password entered by the admin.

## Demo login credentials
The application creates these demo accounts on first startup. On an existing database, the previous seeded `admin` / `Admin@123` account is migrated to `Admin` / `admin@123`; existing student account passwords are preserved:

| Portal | Username | Password |
|---|---|---|
| Library Admin | `Admin` | `admin@123` |
| Student Ananya (seeded demo) | `stu1` | `Student@123` |
| Student Arun (seeded demo) | `stu2` | `Student@123` |

Change demo passwords before using this project with real personal data. Passwords are stored as BCrypt hashes in the `LIBRARY_USERS` table. Login sessions are maintained by the Java backend.

## Login roles
- **Library Admin:** can view the dashboard and reading analysis, add books, register students, issue books, return books and view reports.
- **Student:** can view the book catalogue and only their own borrowing history. Students cannot add books, register users, issue books or return books.

When an admin registers a student, the system assigns a Student ID such as `stu3` and asks for a temporary password (minimum six characters). Students sign in with that ID and password; the admin can find the ID in the student list. Existing database records are assigned IDs from their student record number on startup.

## Database
H2 file database JDBC URL: `jdbc:h2:file:./data/librarydb`
- Username: `sa`
- Password: leave blank
- H2 console: http://localhost:8080/h2-console

Tables: `BOOKS`, `STUDENTS`, `LOANS`, `LIBRARY_USERS`. Hibernate creates/updates runtime tables. The `database/schema.sql` file documents the relational schema.

## Student reading analysis
The admin workspace includes a **Reading Analysis** page with student borrow counts, department share, the monthly trend for the last 12 months, top categories, and the five most active readers. Summary cards show student and borrowing totals, current-month active readers, and average borrows per student. Charts use responsive inline SVG, so no additional npm packages are needed.

The backend exposes these admin-session-protected JSON endpoints:
- `GET /api/analytics/books-per-student`
- `GET /api/analytics/department-reading`
- `GET /api/analytics/monthly-trend`
- `GET /api/analytics/top-categories`
- `GET /api/analytics/top-readers`
- `GET /api/analytics/summary`

Each endpoint accepts optional `department`, `startDate`, and `endDate` query parameters (`YYYY-MM-DD`). Borrow totals count loan/issue records by issue date; the database does not separately record whether a book was actually read.

If Reading Analysis shows an API error after updating the project, stop the existing Java backend and Vite processes, then start them again using the commands above. An old Java process can return 404 for new analytics routes, and an old Vite process may not have loaded the `/api` proxy.

## Important project notes
- The browser does not connect directly to the database.
- Student and analytics APIs, including the reading-pattern endpoint, require the appropriate authenticated session.
- This is a learning prototype, not a production-ready library system. Use HTTPS, stronger validation, CSRF protection, password reset and production secrets before real deployment.
