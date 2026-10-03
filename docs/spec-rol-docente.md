# Spec: Rehacer vistas Rol Docente — Mis cursos + Detalle Curso

> Fase: SPECIFY (2026-10-02). Fuente: pedido humano + respuestas 2026-10-02. Base: `SPEC.md` / `mvp.md` §4.

## Objective

Rehacer por completo las vistas del rol **Docente** (reemplazan `TakeAttendanceView` como vista inicial y de navegación para DOCENTE) con 2 vistas:

1. **Vista 1 — "Mis cursos"** (post-login Docente): lista los cursos/materias asignados al docente **agrupados por fecha de sesión, con la fecha como título de sección**. Ventana: hoy + 5 días siguientes. Si hoy no hay sesión, se muestra mensaje **"Sin sesión hoy"** y se siguen listando las sesiones de mañana hasta +5 días. Cada tarjeta muestra datos relevantes + botón **"Ir a detalle"**.
2. **Vista 2 — "Curso {NOMBRE_DEL_CURSO}"** (tras pulsar "Ir a detalle"): muestra cabecera detalle del curso + nómina de alumnos del curso. Cada fila de alumno muestra: código, nombre completo, asistencias (H asistidas), faltas (H faltas), % asistencia (BR-03), semáforo/margen DPI (BR-06), y **N checkboxes** (uno por hora didáctica del bloque) para marcar asistencia del día.

Quién: rol DOCENTE (solo Mis cursos ↔ Detalle; sin Histórico ni Panel alumno por decisión 2026-10-03). Éxito = docente entra, ve sus cursos, entra a un curso, marca asistencia de hoy con N checkboxes, guarda → sesión DICTADA, %/margen recalculan, draft y export siguen funcionando.

Fuera de alcance en esta iteración: rol Alumno, rol Admin, Histórico/Matriz, cambios de BR-00/BR-03/BR-06.

## Tech Stack

Igual que `SPEC.md` §2 (fijado, sin cambios):
Java 21 + JavaFX 21 (`org.openjfx`) + AtlantaFX 2.0.1 (Primer Dark) + Jackson 2.17 + java-jwt 4.4 + Preferences + Gradle Wrapper. Sin FXML (JavaFX puro, una vista = `XxxView.java`). DTOs como `records`. Sin Tardanza.

## Commands

```bash
./gradlew run              # Dev: lanzar JavaFX (requiere JDK 21)
./gradlew test             # Unit tests (JUnit5 + Mockito + AssertJ)
./gradlew build            # Compila + tests + JaCoCo
./gradlew check            # test + jacocoTestReport
```

## Project Structure

```
src/main/java/com/javapp/
├── attendance/
│   ├── MyCoursesView.java      → NUEVA Vista 1 (secciones por fecha hoy…+5, "Sin sesión hoy", cards + "Ir a detalle", callback onDetalle(ClassSession))
│   ├── CourseDetailView.java   → NUEVA Vista 2 (cabecera + nómina + N checkboxes + guardar/draft/export + botón Volver)
│   └── TakeAttendanceView.java → DEPRECADA para Docente (se mantiene para Admin o se elimina en cleanup)
├── api/
│   ├── ApiClient.java          → + studentsByCourse(courseId): List<Student> + sessionsWindow(from, to)
│   ├── dto/Student.java        → NUEVO record(id, code, fullName)
│   └── MockApiClient.java      → fixtures alumnos demo + sesiones hoy…+5 (ambas materias) + resumen existente
└── App.java                    → router: DOCENTE inicial = MyCoursesView; navegación Mis cursos ↔ Detalle

src/test/java/com/javapp/
├── api/MockApiClientTest.java  → + studentsByCourse, guardar hoy → DICTADA
└── attendance/DocenteViewsTest  → lógica pura (N checkboxes = bloqueHoras, navegación)
```

Convención: Vistas en español en UI ("Mis cursos", "Curso {nombre}", "Ir a detalle"), clases `*View.java`.

## Code Style

```java
// records para DTOs; callbacks para navegación (sin router global)
public record Student(String id, String code, String fullName) {}

public class MyCoursesView extends VBox {
    public MyCoursesView(ApiClient api, Consumer<ClassSession> onDetail) {
        // Secciones por fecha (hoy … hoy+5): título = fecha formato "EEE dd/MM"
        // Si hoy vacío → Label("Sin sesión hoy") + seguir con mañana…+5
        // una Card por sesión: codigo · nombre · seccion · horario · bloqueHoras · estado
        // Button("Ir a detalle") -> onDetail.accept(session)
    }
}
// N checkboxes = session.blockHours(); marcado = Presente, no marcado = Falta (US-08)
```

Reglas SPEC.md §5: lowerCamelCase vars/métodos, UpperCamelCase clases, `com.javapp.*`; sin `System.out`; texto UI en español; Access Token solo RAM.

## Testing Strategy

Framework: JUnit 5 + AssertJ + Mockito (existente). JaCoCo ≥70% en `common/session/api` (sin bajar).

- Unit obligatorio:
  - `studentsByCourse(mat-001)` retorna 3 alumnos con código + nombre no vacío.
  - `sesionHoy(mat-001)` = sesión con fecha == hoy o fallback definido; `blockHours` == Nº checkboxes esperados (2 y 3 en fixtures).
  - Ventana hoy…+5: `sessionsWindow(hoy, hoy+5)` agrupa por fecha; test con hoy vacío → sección hoy vacía + mañana…+5 con sesiones.
  - `saveAttendance(sesionHoy)` con `arr.length == blockHours` → estado DICTADA; longitudes distintas → VALIDATION.
  - `studentSummary` por fila: % BR-03 `6/(10-2)=75%`, riesgo bordes 50/51/99/100%.
- Integración ligera: Mock carga fixtures, Jackson intacto.
- UI: manual `./gradlew run` como `docente/demo`: Mis cursos → Ir a detalle → marcar → guardar → recalcula. Checklist en PR.
- Verify por task: `./gradlew test` verde antes de commit.

## Boundaries

- Always: `./gradlew test` antes de commit; validar `arr.length == bloqueHoras`; recalcular %/margen tras guardar (BR-03/BR-06); error genérico AUTH; Access solo RAM / Refresh solo Preferences; actualizar este spec si cambia decisión.
- Ask first: cambiar contrato `ApiClient` más allá de `studentsByCourse` (aprobado solo eso); añadir dependencia nueva; tocar CI/jpackage; cambiar BR-00/estados; eliminar `TakeAttendanceView`/`AttendanceMatrixView` (de momento solo des-rutear Docente, no borrar).
- Never: commitear secretos/tokens; editar `vendor/` o `.git/`; borrar test fallando sin aprobación; verificar firma JWT en cliente; usar Tardanza en UI/cálculos.

## Success Criteria (testable)

- [ ] Login `docente/demo` aterriza en **"Mis cursos"** con secciones por fecha **hoy … hoy+5**, título de sección = fecha (`EEE dd/MM`, ej. "sáb 03/10"). Cada card muestra código, nombre, sección, horario, bloqueHoras, matriculados, estado sesión + botón **"Ir a detalle"**.
- [ ] Si hoy no hay sesión: sección hoy muestra **"Sin sesión hoy"** y las secciones mañana…+5 siguen listando sus cursos.
- [ ] Pulsar "Ir a detalle" abre **"Curso {NOMBRE}"** con cabecera (nombre, código, sección, horario, docente, matriculados, fecha sesión) + botón Volver + tabla alumnos (3 filas demo) con columnas: código, nombre completo, H asistidas, H faltas, % asistencia, semáforo DPI, N checkboxes.
- [ ] Nº checkboxes por alumno == `bloqueHoras` de la sesión de hoy (fixture: verificar con 2h y 3h).
- [ ] Guardar con todo marcado/desmarcado → sesión hoy pasa a DICTADA, status confirma, `%` y margen recalculan (visible al reabrir).
- [ ] Draft US-09: marcar sin guardar → esperar 60s o forzar → reabrir hoy recupera borrador ("Borrador recuperado").
- [ ] Botón "Exportar Listado a Excel (.xlsx)" genera archivo que abre con alumnos + % + riesgo (US-14).
- [ ] `./gradlew test` verde. Sin regresión Admin/Alumno (Admin conserva acceso a Toma/Histórico).
- [ ] Sin `Tardanza` en UI/cálculos.

## Open Questions (residuales post-respuestas)

1. ~~Sesión "de hoy fija"~~ RESUELTO 2026-10-03: ventana hoy…+5 con títulos por fecha; "Sin sesión hoy" + seguir listando mañana…+5. Detalle abre la sesión pulsada (hoy o futura: futura en solo-lectura o editable según US-08 — propuesta: futura muestra aviso "Sesión futura, aún no editable" y checkboxes deshabilitados).
2. Columnas "porcentaje dpi, etc.": se interpreta como % BR-03 + semáforo BR-06 + margen restante. Confirmar si falta columna (ej. justificadas/exentas visibles).
3. Contador matriculados y horario en tarjeta: ¿`schedule` actual de `Course` basta o pedir bloques por día?
4. Cleanup: ¿eliminar `TakeAttendanceView` en esta misma tarea o dejarla para Admin y borrar después? Propuesta: no borrar, solo des-rutear Docente.

---
**ASSUMPTIONS VALIDADAS 2026-10-02 (respuestas humano):** ampliar mock con DTO Student sí · reemplazo total para Docente sí · conservar Guardar→Dictada + draft US-09 + export US-14 · sesión = hoy fija (sin combo de fechas).
