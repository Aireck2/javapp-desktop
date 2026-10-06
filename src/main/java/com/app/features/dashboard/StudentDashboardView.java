package com.app.features.dashboard;

import com.app.api.ApiClient;
import com.app.shared.fxml.FxmlViewLoader;
import javafx.scene.layout.VBox;

/** FXML-backed student dashboard screen. */
public final class StudentDashboardView extends VBox {

  public StudentDashboardView(ApiClient api, String studentId) {
    StudentDashboardViewModel viewModel =
        new StudentDashboardViewModel(new StudentDashboardService(api, studentId));
    FxmlViewLoader.loadInto(
        this,
        "/com/app/features/dashboard/student-dashboard.fxml",
        new StudentDashboardController(viewModel));
  }
}
