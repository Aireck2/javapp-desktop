package com.app.components;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.layout.SharedPortalShell;
import com.app.models.CourseModel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ComponentsLayoutTest {

  @BeforeAll
  static void initJavaFx() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException ignored) {
      // Toolkit already started
    }
  }

  @Test
  @DisplayName("AppHeader inicializa con subtítulo y dispara callback de avatar")
  void appHeaderInitializesAndTriggersAvatarCallback() {
    AtomicBoolean avatarClicked = new AtomicBoolean(false);
    AppHeader header = new AppHeader("PORTAL DOCENTE", () -> avatarClicked.set(true));

    assertThat(header).isNotNull();
    assertThat(header.getChildren()).isNotEmpty();
    assertThat(header.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));
  }

  @Test
  @DisplayName("CourseCard para docente con 'Clase Hoy' renderiza y responde a eventos")
  void courseCardTeacherWithTodayClass() {
    CourseModel model =
        new CourseModel(
            "c-01",
            "CS-101",
            "SEC-A",
            "Programación Java",
            "08:00 - 10:00",
            "Lab-102",
            "Clase Hoy",
            28,
            12,
            40);

    AtomicReference<CourseModel> clickedModel = new AtomicReference<>();
    CourseCard card = new CourseCard(model, true, clickedModel::set);

    assertThat(card.getCourse()).isEqualTo(model);
    assertThat(card.isTeacherRole()).isTrue();
    assertThat(card.getChildren()).hasSize(5);
  }

  @Test
  @DisplayName("CourseCard para alumno muestra 'Ver Curso' y métricas adecuadas")
  void courseCardStudentRole() {
    CourseModel model =
        new CourseModel(
            "c-02",
            "MAT-201",
            "SEC-B",
            "Cálculo II",
            "10:00 - 12:00",
            "Aula 301",
            "Teoría",
            35,
            10,
            40);

    CourseCard card = new CourseCard(model, false);

    assertThat(card.getCourse().name()).isEqualTo("Cálculo II");
    assertThat(card.isTeacherRole()).isFalse();
  }

  @Test
  @DisplayName("BottomNavBar alterna pestañas activas y notifica cambios")
  void bottomNavBarSelection() {
    AtomicReference<BottomNavBar.TabItem> selectedTab = new AtomicReference<>();
    BottomNavBar nav = new BottomNavBar(true, selectedTab::set);

    assertThat(nav.getCurrentActiveTab()).isEqualTo(BottomNavBar.TabItem.ATTENDANCE);
    assertThat(nav.isTeacher()).isTrue();
    assertThat(nav.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));

    nav.setActiveTab(BottomNavBar.TabItem.SCHEDULE);
    assertThat(nav.getCurrentActiveTab()).isEqualTo(BottomNavBar.TabItem.SCHEDULE);
  }

  @Test
  @DisplayName("SharedPortalShell envuelve header, content y bottom bar")
  void sharedPortalShellStructure() {
    SharedPortalShell shell = new SharedPortalShell("PORTAL ALUMNO", false);
    Label sampleContent = new Label("Contenido de vista");

    shell.setViewContent(sampleContent);
    shell.setPortalTitle("PORTAL DOCENTE");

    assertThat(shell.getHeader()).isNotNull();
    assertThat(shell.getHeader().getSubtitle()).isEqualTo("PORTAL DOCENTE");
    assertThat(shell.getBottomNavBar()).isNotNull();
    assertThat(shell.getScrollContent().getContent()).isEqualTo(sampleContent);
    assertThat(shell.getStylesheets()).anyMatch(path -> path.endsWith("/com/app/css/theme.css"));
  }

  @Test
  @DisplayName("MainShell hereda de SharedPortalShell y orquesta el ScreenRouter")
  void mainShellInheritsSharedPortalShellAndIntegratesRouter() {
    com.app.api.MockApiClient api = new com.app.api.MockApiClient();
    AtomicBoolean loggedOut = new AtomicBoolean(false);

    com.app.layout.MainShell shell =
        new com.app.layout.MainShell(api, () -> loggedOut.set(true), "PORTAL DOCENTE", true);

    assertThat(shell).isInstanceOf(SharedPortalShell.class);
    assertThat(shell.getHeader()).isNotNull();
    assertThat(shell.getHeader().getSubtitle()).isEqualTo("PORTAL DOCENTE");
    assertThat(shell.getBottomNavBar()).isNotNull();
    assertThat(shell.getInnerRouter()).isNotNull();
    assertThat(shell.getScrollContent().getContent()).isNotNull();
  }
}
