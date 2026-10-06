package com.app.features.attendance;

import com.app.api.ApiClient;
import com.app.api.dto.ClassSession;
import com.app.navigation.ScreenLifecycle;
import com.app.shared.fxml.FxmlViewLoader;
import javafx.scene.layout.VBox;

/** FXML-backed attendance detail screen. */
public final class AttendanceDetailView extends VBox implements ScreenLifecycle {

  private final AttendanceDetailController controller;

  public AttendanceDetailView(ApiClient api, ClassSession session, Runnable onBack) {
    AttendanceDetailViewModel viewModel = new AttendanceDetailViewModel(new AttendanceService(api));
    controller = new AttendanceDetailController(viewModel, session, onBack);
    FxmlViewLoader.loadInto(
        this, "/com/app/features/attendance/attendance-detail.fxml", controller);
  }

  @Override
  public void onHidden() {
    controller.onHidden();
  }
}
