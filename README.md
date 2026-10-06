<div align="center">

<img src="src/main/resources/images/logo.png" alt="Javapp Desktop logo" width="88" />

# Javapp Desktop

**Course management and attendance tracking for desktop**

[![Build](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml/badge.svg)](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-5382a1?style=flat-square)
![macOS and Windows](https://img.shields.io/badge/platform-macOS%20%7C%20Windows-lightgrey?style=flat-square)

[Features](#features) · [Run locally](#run-locally) · [Configuration](#configuration) · [Development](#development) · [Project structure](#project-structure)

</div>

Javapp Desktop is a JavaFX application for academic attendance workflows. It provides role-aware views for administrators, teachers, and students, backed by an in-memory mock API so the app can run without a server.

> [!NOTE]
> The current build uses `MockApiClient`; no production backend is connected. `ApiClient` defines the contract for a future HTTP implementation.

## Features

- **Sign in and session restore:** splash screen checks a saved refresh token; login supports a “remember me” option and inactivity logout.
- **Course timeline:** administrators and teachers see sessions grouped by day, from today through the next five days.
- **Attendance entry:** mark students by teaching hour, search and filter the roster, apply bulk marks, add an observation, and submit attendance.
- **Draft recovery:** unsaved attendance is saved in memory every 60 seconds and can be restored later the same day. Drafts are lost when the app closes.
- **Student dashboard:** view course attendance, remaining absence allowance, and risk level.
- **Profile:** view the signed-in user’s name, username, and roles.
- **FXML and CSS UI:** screens and shared portal components use FXML layouts with a shared application stylesheet.

## Demo accounts

Use password `demo` with any of these accounts:

| Username | Role | Initial view |
| --- | --- | --- |
| `admin` | Administrator | Course timeline |
| `docente` | Teacher | Course timeline |
| `alumno` | Student | Attendance dashboard |

The mock data is for local demonstration only; it does not enforce production authorization or persist attendance across app restarts.

## Run locally

### Requirements

- JDK 21. The Gradle wrapper is included; a separate Gradle installation is not needed.
- Git, to clone the repository.

```bash
git clone https://github.com/Aireck2/javapp-desktop.git
cd javapp-desktop
./gradlew run
```

On Windows, use `gradlew.bat run` in PowerShell or Command Prompt.

## Configuration

Set values as Java system properties (`-Dname=value`) or environment variables. System properties take precedence.

| Property | Environment variable | Default | Description |
| --- | --- | --- | --- |
| `javapp.inactivityMinutes` | `JAVAPP_INACTIVITY_MINUTES` | `15` | Idle time before automatic logout. |
| `javapp.maxFaltas` | `JAVAPP_MAX_FALTAS` | `0.30` | Maximum absence ratio used to calculate the course absence allowance. |
| `javapp.splashMinMillis` | `JAVAPP_SPLASH_MIN_MILLIS` | `5000` | Minimum time the splash screen remains visible. |
| `javapp.baseUrl` | `JAVAPP_BASE_URL` | `http://localhost:8080/api` | Reserved for a future HTTP client; unused by the current mock. |

## Attendance rules

- Attendance percentage excludes justified and exempt hours from its denominator.
- The absence allowance is required course hours multiplied by the configured maximum absence ratio.
- Risk is green through 50% of the allowance, amber above 50% and below 100%, and red at 100% or more. Justified and exempt hours do not consume the allowance.
- Future sessions and sessions outside the 60-day editable period are read-only in attendance entry.

## Development

The project uses Gradle, Java 21, JavaFX 21, AtlantaFX, and Ikonli. Tests use JUnit 5, Mockito, and AssertJ.

```bash
./gradlew compileJava   # Compile application sources
./gradlew test          # Run tests
./gradlew check         # Run verification tasks and generate JaCoCo reports
./gradlew spotlessApply # Format Java, FXML, and CSS
./gradlew spotlessCheck # Check formatting without changing files
```

The JaCoCo HTML report is written to `build/reports/jacoco/test/html/index.html`.

### Package an installer

Build native installers on their target operating system with JDK 21:

```bash
./gradlew jpackageDmg  # macOS DMG
./gradlew jpackageExe  # Windows EXE
./gradlew jpackageMsi  # Windows MSI
```

Installers are written to `build/jpackage/`. Windows packaging requires WiX Toolset 3 or later. GitHub Actions builds and tests on macOS and Windows, then packages a DMG or EXE on the matching runner.

## Project structure

```text
src/main/java/com/app/
├── api/                 # API contract, mock implementation, errors, and DTOs
│   └── dto/
├── auth/                # Login session state and refresh-token storage
├── common/              # Attendance business calculations and risk levels
├── components/          # Reusable JavaFX components
├── config/              # Application configuration
├── features/            # Auth, courses, attendance, dashboard, and profile flows
├── layout/              # Shared shell and role-based navigation
├── mappers/              # API DTO to presentation-model mapping
├── models/               # UI presentation models
├── navigation/           # Screen router and lifecycle hooks
└── shared/               # FXML loading support

src/main/resources/com/app/
├── css/                  # Application theme
├── features/             # Feature screen FXML
└── shared/components/    # Shared shell, header, and navigation FXML
```

## Troubleshooting

- **JavaFX does not start:** confirm the app is running with JDK 21; JavaFX modules are supplied by Gradle.
- **`jpackage` is unavailable:** use a full JDK 21, which includes `jpackage`.
- **A test leaves Java Preferences behind:** `TokenStoreTest` uses the `com/javapp/desktop-test-tokenstore` node. On macOS, remove it with `defaults delete com.javapp.desktop-test-tokenstore`.
