# javapp-desktop

<p align="center">
  <img src="src/main/resources/images/logo.png" alt="javapp-desktop logo" width="120" />
</p>

[![Build](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml/badge.svg)](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-5382a1?style=flat-square)
![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows-lightgrey?style=flat-square)

Native desktop client (JavaFX) for the academic attendance control system: role-based login, teacher course views with per-hour attendance marking, student margin dashboard, and attendance history matrix.

> [!NOTE]
> This MVP runs against an in-memory `MockApiClient` — no backend required. The `ApiClient` interface is ready for a real `HttpApiClient` implementation.

## Features

- **Role-based access (US-01)**: splash with refresh-token auto-login, generic error on bad credentials, auto-logout on inactivity. Each role lands on its own home view.
- **Teacher — Mis cursos**: sessions grouped by date (today through +5 days, date as section title). Empty today shows "Sin sesión hoy" while upcoming days keep listing. Each card shows course code, section, schedule, hour block, status, enrollment count, and an "Ir a detalle" button.
- **Teacher — Curso {nombre}**: course header plus the class roster (code, full name, attended hours, missed hours, % attendance, DPI risk semaphore with remaining margin). One checkbox per teaching hour — checked is `Presente`, unchecked is `Falta`. Saving marks the session `Dictada`. Future sessions are read-only. Includes 60-second draft autosave with same-day restore (US-09) and one-click `.xlsx` export (US-14).
- **Student margin dashboard (US-15)**: per-course attendance %, remaining miss margin, and risk semaphore (green / amber / red → DPI). `Falta Justificada` and `Exento` never consume margin.
- **History matrix (US-16, Admin)**: students × dates grid with color coding, detail tooltips, and pending-session markers.

## Prerequisites

- JDK 21 (e.g. `brew install --cask temurin@21`). No global Gradle needed — the wrapper is included.

> [!IMPORTANT]
> JavaFX 21 requires JDK 21. Running with another JDK will fail at startup.

## Getting started

```bash
git clone git@github.com:Aireck2/javapp-desktop.git
cd javapp-desktop
./gradlew run
```

Log in with a demo account (password is `demo` for all):

| User      | Role    | Sees                                    |
| --------- | ------- | --------------------------------------- |
| `admin`   | ADMIN   | Take attendance, history, student panel |
| `docente` | DOCENTE | Mis cursos only                         |
| `alumno`  | ALUMNO  | Personal dashboard only                 |

> [!TIP]
> Log in as `docente`, open a course from "Mis cursos", save attendance on today's session, then log in as `alumno` — the % and semaphore recalculate immediately.

## Commands

```bash
./gradlew run          # Launch the app
./gradlew test         # Unit tests
./gradlew build        # Compile + tests + coverage report
./gradlew jpackageDmg  # macOS installer (requires build first)
./gradlew jpackageExe  # Windows installer (run on Windows / CI)
```

## Configuration

| Property / env var                                       | Default                     | Purpose                                 |
| -------------------------------------------------------- | --------------------------- | --------------------------------------- |
| `javapp.baseUrl` / `JAVAPP_BASE_URL`                     | `http://localhost:8080/api` | Reserved for the future `HttpApiClient` |
| `javapp.inactivityMinutes` / `JAVAPP_INACTIVITY_MINUTES` | `15`                        | Auto-logout after inactivity (US-01)    |
| `javapp.maxFaltas` / `JAVAPP_MAX_FALTAS`                 | `0.30`                      | Max miss ratio per cycle (BR-06)        |

## Project structure

```
src/main/java/com/javapp/
├── App.java                 # Shell, AtlantaFX theme, role router, inactivity watchdog
├── session/UserSession.java # RAM-only singleton: token, groups, RBAC checks
├── auth/                    # SplashView, LoginView, TokenStore (Preferences refresh token)
├── api/                     # ApiClient interface, MockApiClient, dto/* records
├── attendance/              # MyCoursesView + CourseDetailView (teacher), TakeAttendanceView + AttendanceMatrixView (admin)
├── dashboard/               # StudentDashboardView (US-15)
├── common/                  # BrCalculations (BR-03/BR-06), RiskLevel, AttendanceExport (.xlsx)
└── config/AppConfig.java    # Centralized settings with env overrides
src/test/java/com/javapp/    # Unit tests mirroring the main tree
```

Key specs: `SPEC.md` (source of truth), `mvp.md` (product detail), `requirements.md` (backlog + business rules), `docs/spec-rol-docente.md` (teacher views spec).

## API contract

`ApiClient` is the seam between UI and data. `MockApiClient` implements it in memory; a future `HttpApiClient` must honor the same interface without changing views.

| Method                          | Future REST equivalent                                  | Rules                                                                      |
| ------------------------------- | ------------------------------------------------------- | -------------------------------------------------------------------------- |
| `login` / `refresh`             | `POST /api/v1/auth/login` · `POST /api/v1/auth/refresh` | Generic `AUTH` error on bad credentials; refresh `401` falls back to login |
| `coursesForCurrentUser`        | `GET /api/v1/{rol}/materias`                            | Course cards with section, teacher, schedule                               |
| `studentsByCourse`              | `GET /materias/{id}/alumnos`                            | Roster with institutional code + full name                                 |
| `sessionsWindow(from, to)`      | `GET /materias/sesiones?desde&hasta`                    | Today through +5 days, excludes `Feriada`/`Suspendida`, sorted by date     |
| `validSessions(courseId)`       | `GET /materias/{id}/sesiones?validas`                   | Past/today only, `Programada` or `Dictada`                                 |
| `studentSummary`                | `GET /materias/{id}/alumnos/{alu}/resumen`              | % via BR-03, margin + risk via BR-06                                       |
| `saveAttendance`                | `POST /sesiones/{id}/asistencia`                        | `arr.length` must equal block hours; marks session `Dictada`               |

Errors surface as `ApiException(Kind: AUTH | VALIDATION | NOT_FOUND)`.

## Business rules (summary)

- **BR-00**: session states `Programada → Dictada | Feriada | Suspendida`; per-student states `Presente | Falta | Falta Justificada | Exento`. No `Tardanza`.
- **BR-03**: `% = H_asistidas / (H_dictadas − H_justificadas − H_exentas) × 100`. Example: `6 / (10 − 2) = 75%`.
- **BR-06**: `margin = H_exigibles × %máx_faltas`; consumption = `Falta` hours. Green ≤ 50%, amber 51–99%, red ≥ 100% → DPI.

## Testing

JUnit 5 + Mockito + AssertJ, with JaCoCo coverage (target ≥ 70% on `common` / `session` / `api`).

```bash
./gradlew test   # must be green before every commit
```

Covered: login/JWT claims, refresh renew + `401`, valid-date filtering, save → `Dictada` + recalculation, future-date rejection, `Justificada`/`Exento` exclusions, semaphore edges (50 / 51 / 99 / 100%), roster + date-window fixtures, `.xlsx` export. UI is verified manually per role (`./gradlew run` checklist in `docs/spec-rol-docente.md`).

## Packaging

`jpackage` ships with JDK 21 (full JDK, not JRE — `jpackage --version` should print `21.x`). Build first, then package for the host OS. Windows installers are produced via CI.

## Troubleshooting

- **`./gradlew` fails with "no Gradle build"**: you need `settings.gradle.kts` at the repo root — pull the latest `develop`.
- **Blank window / CSS warnings on startup**: AtlantaFX logs them harmlessly if the theme JAR resolves slowly; the stylesheet still applies.
- **`jpackage` not found**: install a full JDK 21 (not JRE).
- **Tests fail on `TokenStoreTest`**: it uses the `com/javapp/desktop-test-tokenstore` Preferences node — clear it with `defaults delete com.javapp.desktop-test-tokenstore` on macOS.
