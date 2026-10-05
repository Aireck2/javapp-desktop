<div align="center">

<img src="src/main/resources/images/logo.png" alt="Javapp Desktop logo" width="88" />

# Javapp Desktop

*Desktop app for academic attendance and course management*

[![Build](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml/badge.svg)](https://github.com/Aireck2/javapp-desktop/actions/workflows/build.yml)
![Java 21](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk&logoColor=white)
![JavaFX 21](https://img.shields.io/badge/JavaFX-21-5382a1?style=flat-square)
![Desktop](https://img.shields.io/badge/platform-macOS%20%7C%20Windows-lightgrey?style=flat-square)

[Features](#features) · [Getting started](#getting-started) · [Configuration](#configuration) · [Project structure](#project-structure) · [Build and test](#build-and-test)

</div>

Javapp Desktop is a JavaFX client for tracking class attendance. It includes role-based portals for administrators, teachers, and students, with course views, per-hour attendance entry, and attendance summaries.

> [!NOTE]
> The app currently uses an in-memory `MockApiClient`; it does not require a backend. `ApiClient` defines the contract for a future service implementation.

## Features

- **Role-based access:** splash screen with refresh-token sign-in, login, and automatic logout after inactivity.
- **Course timeline:** view sessions from today through the next five days, grouped by date, with course and session details.
- **Attendance entry:** mark each student present or absent by teaching hour, search and filter the roster, apply bulk marks, and save the session.
- **Draft recovery:** attendance changes autosave every 60 seconds and can be restored during the same day. Drafts are held in memory and do not survive an app restart.
- **Student dashboard:** course attendance percentages, absence margin, and risk level. Justified absences and exempt hours do not consume the margin.
- **User profile:** view the signed-in user's name, username, and roles.

## Getting started

### Requirements

- JDK 21. A full JDK is needed to build native installers with `jpackage`.
- Git.
- No system Gradle installation is needed; use the included wrapper.

### Run locally

```bash
git clone https://github.com/Aireck2/javapp-desktop.git
cd javapp-desktop
./gradlew run
```

On Windows, run `gradlew.bat run` from PowerShell or Command Prompt.

### Demo accounts

All demo accounts use the password `demo`.

| Username | Role | Start page |
| --- | --- | --- |
| `admin` | Administrator | Course timeline and attendance workflow |
| `docente` | Teacher | Course timeline and attendance workflow |
| `alumno` | Student | Personal attendance dashboard |

## Configuration

Settings can be provided as Java system properties (`-D...`) or environment variables.

| Setting | Environment variable | Default | Purpose |
| --- | --- | --- | --- |
| `javapp.baseUrl` | `JAVAPP_BASE_URL` | `http://localhost:8080/api` | Reserved for a future HTTP client |
| `javapp.inactivityMinutes` | `JAVAPP_INACTIVITY_MINUTES` | `15` | Idle time before automatic logout |
| `javapp.maxFaltas` | `JAVAPP_MAX_FALTAS` | `0.30` | Maximum absence ratio used for the risk margin |
| `javapp.splashMinMillis` | `JAVAPP_SPLASH_MIN_MILLIS` | `5000` | Minimum time the splash screen is shown |

## Attendance rules

- Session states are `PROGRAMADA`, `DICTADA`, `FERIADA`, and `SUSPENDIDA`. Future sessions are read-only in attendance entry.
- Attendance percentage is calculated from attended hours after excluding justified and exempt hours from the denominator.
- The absence margin is total required course hours multiplied by the configured maximum absence ratio.
- Risk is green through 50% of the margin, amber above 50% and below 100%, and red at 100% or more. Justified and exempt hours do not consume the margin.

## Project structure

```text
src/main/java/com/app/
├── api/          # ApiClient, in-memory mock, errors, and DTOs
├── attendance/  # Course timeline, course detail, and attendance model mapping
├── auth/         # Splash flow and refresh-token storage
├── common/       # Attendance calculations and risk levels
├── components/   # Shared header, navigation, and course/student cards
├── dashboard/   # Student attendance dashboard
├── layout/       # Shared portal shell and role-based routing
├── models/       # UI presentation models
├── navigation/  # Screen router
└── views/        # Login, profile, and attendance detail views
```

## Build and test

```bash
./gradlew compileJava  # Compile the application
./gradlew test         # Run unit tests
./gradlew build        # Build and run verification tasks
```

GitHub Actions builds and tests on macOS and Windows. It also packages a DMG on macOS and Windows installers on Windows.

To create an installer locally, use JDK 21 on the target platform:

```bash
./gradlew jpackageDmg  # macOS
./gradlew jpackageExe  # Windows
```

Installers are written to `build/jpackage/`.

## Troubleshooting

- **`jpackage` is missing:** install a full JDK 21 and check `jpackage --version`.
- **JavaFX startup fails:** verify the app is running with JDK 21; JavaFX 21 is configured by the Gradle plugin.
- **A test leaves Java Preferences behind:** `TokenStoreTest` uses the `com/javapp/desktop-test-tokenstore` node. On macOS, remove it with `defaults delete com.javapp.desktop-test-tokenstore`.
