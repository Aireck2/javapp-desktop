package com.app.common;

import com.app.api.dto.ClassSession;
import com.app.api.dto.ClassSession.SessionStatus;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * US-14: consolidated-matrix export to Excel (.xlsx). Pure, no JavaFX.
 */
public final class AttendanceExport {

    /** One roster row for the consolidated matrix. */
    public record StudentRow(
            String studentId, String name,
            double attendedHours, double missedHours, double percentage, String risk) {}

    private AttendanceExport() {}

    public static void exportMatriz(
            String courseCode,
            List<ClassSession> sessions,
            List<StudentRow> rows,
            File file) throws IOException {
        try (var out = new FileOutputStream(file)) {
            exportMatriz(courseCode, sessions, rows, out);
        }
    }

    public static void exportMatriz(
            String courseCode,
            List<ClassSession> sessions,
            List<StudentRow> rows,
            OutputStream out) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sh = wb.createSheet("Asistencia " + courseCode);
            int c = 0;
            Row head = sh.createRow(0);
            c = cell(head, c, "Alumno");
            c = cell(head, c, "Nombre");
            for (ClassSession s : sessions) {
                c = cell(head, c, s.date() + " (" + s.blockHours() + "h)");
            }
            c = cell(head, c, "H asistidas");
            c = cell(head, c, "Faltas");
            c = cell(head, c, "% asistencia");
            cell(head, c, "Riesgo DPI");

            int r = 1;
            for (StudentRow f : rows) {
                Row row = sh.createRow(r++);
                int cc = 0;
                cc = cell(row, cc, f.studentId());
                cc = cell(row, cc, f.name());
                for (ClassSession s : sessions) {
                    cc = cell(row, cc,
                            s.status() == SessionStatus.DICTADA
                                    ? s.blockHours() + "h" : "pend.");
                }
                cc = num(row, cc, f.attendedHours());
                cc = num(row, cc, f.missedHours());
                cc = num(row, cc, Math.round(f.percentage() * 10.0) / 10.0);
                cell(row, cc, f.risk());
            }
            for (int i = 0; i <= sessions.size() + 6; i++) {
                sh.autoSizeColumn(i);
            }
            wb.write(out);
        }
    }

    private static int cell(Row row, int col, String v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v);
        return col + 1;
    }

    private static int num(Row row, int col, double v) {
        Cell cell = row.createCell(col);
        cell.setCellValue(v);
        return col + 1;
    }
}
