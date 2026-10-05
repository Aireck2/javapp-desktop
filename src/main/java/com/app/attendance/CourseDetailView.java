package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.dto.ClassSession;
import com.app.views.AttendanceDetailView;

/** Keeps the existing course-detail route while delegating to the attendance detail screen. */
public final class CourseDetailView extends AttendanceDetailView {

    public CourseDetailView(ApiClient api, ClassSession session, Runnable onBack) {
        super(api, session, onBack);
    }
}
