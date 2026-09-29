# javapp-desktop

[![Build](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml/badge.svg)](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-5382a1?style=flat-square)
![Platform](https://img.shields.io/badge/platform-macOS%20%7C%20Windows-lightgrey?style=flat-square)

Native desktop client (JavaFX) for the academic attendance control system: role-based login, guided attendance taking, student margin dashboard, and attendance history matrix.

> [!NOTE]
> This MVP runs against an in-memory `MockApiClient` — no backend required. The `ApiClient` interface is ready for a real `HttpApiClient` implementation.

## Features

- **Role-based access (US-01)**: Admin gets the full menu; Docente gets attendance + history + personal panel; Alumno gets only the personal dashboard. Generic error message on bad credentials, logout on inactivity.
- **Attendance taking (US-08)**: only valid dates are listed (no future, no `Feriada`/`Suspendida` sessions). One checkbox per teaching hour — checked is `Presente`, unchecked is `Falta`. Saving marks the session `Dictada`.
- **Student margin dashboard (US-15)**: per-course attendance %, remaining miss margin, and risk semaphore (green / amber / red → DPI). `Falta Justificada` and `Exento` never consume margin.
- **History matrix (US-16)**: students × dates grid with color coding, detail tooltips, and pending-session markers.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Java 21 LTS (records, `HttpClient`, virtual threads) |
| UI | JavaFX 21 + AtlantaFX 2.x (Primer Dark theme) |
| JSON / JWT | Jackson Databind 2.17, Auth0 java-jwt 4.x (claim inspection only) |
| Secure storage | `java.util.prefs.Preferences` (refresh token), RAM-only access token |
| Build / packaging | Gradle 8.10 (wrapper) + `jpackage` (`.dmg`, `.exe`/`.msi`) |
| Tests | JUnit 5, Mockito, AssertJ, JaCoCo |

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

| User | Role | Sees |
|---|---|---|
| `admin` | ADMIN | Full menu |
| `docente` | DOCENTE | Take attendance, history, personal panel |
| `alumno` | ALUMNO | Personal dashboard only |

> [!TIP]
> Try saving attendance as `docente` on a `PROGRAMADA` session, then check the `alumno` dashboard — the % and semaphore recalculate immediately.

## Commands

```bash
./gradlew run          # Launch the app
./gradlew test         # Unit tests
./gradlew build        # Compile + tests + coverage report
./gradlew jpackageDmg  # macOS installer (requires build first)
./gradlew jpackageExe  # Windows installer (run on Windows / CI)
```

## Configuration

| Property / env var | Default | Purpose |
|---|---|---|
| `javapp.baseUrl` / `JAVAPP_BASE_URL` | `http://localhost:8080/api` | Reserved for the future `HttpApiClient` |
| `javapp.inactivityMinutes` / `JAVAPP_INACTIVITY_MINUTES` | `15` | Auto-logout after inactivity (US-01) |
| `javapp.maxFaltas` / `JAVAPP_MAX_FALTAS` | `0.30` | Max miss ratio per cycle (BR-06) |

## Project structure

```
src/main/java/com/javapp/
├── App.java                 # Shell, AtlantaFX theme, role router, inactivity watchdog
├── session/UserSession.java # RAM-only singleton: token, groups, RBAC checks
├── auth/                    # LoginView, TokenStore (Preferences refresh token)
├── api/                     # ApiClient interface, MockApiClient, dto/* records
├── attendance/              # TakeAttendanceView (US-08), AttendanceMatrixView (US-16)
├── dashboard/               # StudentDashboardView (US-15)
├── common/                  # BrCalculations (BR-03/BR-06), RiskLevel
└── config/AppConfig.java    # Centralized settings with env overrides
src/test/java/com/javapp/    # Unit tests mirroring the main tree
```

## Business rules (summary)

- **BR-00**: session states `Programada → Dictada | Feriada | Suspendida`; per-student states `Presente | Falta | Falta Justificada | Exento`. No `Tardanza`.
- **BR-03**: `% = H_asistidas / (H_dictadas − H_justificadas − H_exentas) × 100`. Example: `6 / (10 − 2) = 75%`.
- **BR-06**: `margin = H_exigibles × %máx_faltas`; consumption = `Falta` hours. Green ≤ 50%, amber 51–99%, red ≥ 100% → DPI.

## Roadmap (out of MVP scope)

Admin master-data CRUDs (users, profiles, courses, cycles, schedules, enrollments), draft autosave, attendance reconsideration, holidays/suspensions editor, justifications, executive dashboards, and `.xlsx` export (US-02–US-07, US-09–US-14).

## Troubleshooting

- **`./gradlew` fails with "no Gradle build"**: you need `settings.gradle.kts` at the repo root — pull the latest `develop`.
- **Blank window / CSS warnings on startup**: AtlantaFX logs them harmlessly if the theme JAR resolves slowly; the Primer Dark stylesheet still applies.
- **`jpackage` not found**: install a full JDK 21 (not JRE) — `jpackage --version` should print `21.x`.
- **Tests fail on `TokenStoreTest`**: it uses the `com/javapp/desktop-test-tokenstore` Preferences node — clear it with `defaults delete com.javapp.desktop-test-tokenstore` on macOS.
