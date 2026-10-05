package com.app.attendance;

import com.app.api.ApiClient;
import com.app.api.ApiException;
import com.app.api.dto.Course;
import com.app.api.dto.Student;
import com.app.api.dto.StudentSummary;
import com.app.models.StudentAttendanceModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Builds the presentation models used by both attendance entry points. */
public final class StudentAttendanceModels {

    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*[–-]\\s*(\\d{1,2}):(\\d{2})");

    private StudentAttendanceModels() {}

    public static List<StudentAttendanceModel> load(
            ApiClient api,
            Course course,
            int blockHours,
            Map<String, boolean[]> savedMarks,
            Map<String, boolean[]> draftMarks) throws ApiException {
        List<Student> roster = api.studentsByCourse(course.id());
        List<String> times = hourLabels(course.schedule(), blockHours);
        List<StudentAttendanceModel> models = new ArrayList<>(roster.size());

        for (int i = 0; i < roster.size(); i++) {
            Student student = roster.get(i);
            StudentSummary summary = api.studentSummary(course.id(), student.id());
            boolean exempt = summary.exemptHours() > 0;
            boolean[] marks = draftMarks.get(student.id());
            if (marks == null) {
                marks = savedMarks.get(student.id());
            }
            List<StudentAttendanceModel.HourBlock> blocks = new ArrayList<>(blockHours);
            for (int hour = 0; hour < blockHours; hour++) {
                boolean checked = marks != null && hour < marks.length ? marks[hour] : true;
                blocks.add(new StudentAttendanceModel.HourBlock(times.get(hour), checked));
            }

            long presentBlocks = blocks.stream().filter(StudentAttendanceModel.HourBlock::isChecked).count();
            String badge = exempt ? "Exento RR-04"
                    : presentBlocks == blocks.size() ? "Presente Completo"
                    : presentBlocks == 0 ? "Falta Total" : "Parcial (" + presentBlocks + "h / " + blockHours + "h)";
            String note = exempt ? "Exonerado/a · " + Math.round(summary.exemptHours()) + " h no computables" : null;
            int attendancePercent = (int) Math.round(summary.percentage());
            int absencePercent = Math.max(0, 100 - attendancePercent);
            models.add(new StudentAttendanceModel(
                    String.format("%02d", i + 1), student.id(), student.fullName(), student.code(), badge, blocks,
                    (int) Math.round(summary.taughtHours()), (int) Math.round(summary.attendedHours()),
                    (int) Math.round(summary.missedHours()), attendancePercent, absencePercent, note));
        }
        return models;
    }

    private static List<String> hourLabels(String schedule, int blockHours) {
        Matcher matcher = TIME_RANGE.matcher(schedule == null ? "" : schedule);
        List<String> labels = new ArrayList<>(blockHours);
        if (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            int minute = Integer.parseInt(matcher.group(2));
            for (int i = 0; i < blockHours; i++) {
                int start = (hour * 60 + minute + i * 60) % (24 * 60);
                int end = (start + 60) % (24 * 60);
                labels.add(String.format("%02d:%02d–%02d:%02d", start / 60, start % 60, end / 60, end % 60));
            }
        } else {
            for (int i = 0; i < blockHours; i++) {
                labels.add("Hora " + (i + 1));
            }
        }
        return labels;
    }
}
