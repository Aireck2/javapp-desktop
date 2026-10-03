package com.javapp.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.javapp.api.dto.ClassSession;
import com.javapp.api.dto.ClassSession.SessionStatus;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class AttendanceExportTest {

    @Test
    void export_buildsReadableXlsx_withHeaderAndRows() throws Exception {
        var sessions = List.of(
                new ClassSession("ses-01", "mat-001", LocalDate.now().minusDays(7), 2,
                        SessionStatus.DICTADA),
                new ClassSession("ses-02", "mat-001", LocalDate.now().minusDays(1), 2,
                        SessionStatus.PROGRAMADA));
        var rows = List.of(
                new AttendanceExport.StudentRow("alu-01", "Al Alumno", 2, 2, 50.0, "VERDE"));

        var out = new ByteArrayOutputStream();
        AttendanceExport.exportMatriz("MAT-101", sessions, rows, out);
        assertThat(out.size()).isGreaterThan(0);

        try (var wb = new XSSFWorkbook(new java.io.ByteArrayInputStream(out.toByteArray()))) {
            var sh = wb.getSheetAt(0);
            assertThat(sh.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Alumno");
            assertThat(sh.getLastRowNum()).isEqualTo(1);
            assertThat(sh.getRow(1).getCell(0).getStringCellValue()).isEqualTo("alu-01");
        }
    }
}
