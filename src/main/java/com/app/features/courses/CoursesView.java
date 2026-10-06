package com.app.features.courses;

import com.app.api.ApiClient;
import com.app.api.dto.ClassSession;
import com.app.shared.fxml.FxmlViewLoader;
import java.util.function.Consumer;
import javafx.scene.layout.VBox;

/** FXML-backed view for the teacher course timeline. */
public final class CoursesView extends VBox {

  public CoursesView(
      ApiClient api, Consumer<ClassSession> onDetail, Consumer<ClassSession> onTakeAttendance) {
    CoursesService service = new CoursesService(api);
    CoursesViewModel viewModel = new CoursesViewModel(service);
    CoursesController controller = new CoursesController(viewModel, onDetail, onTakeAttendance);
    FxmlViewLoader.loadInto(this, "/com/app/features/courses/courses.fxml", controller);
  }
}
