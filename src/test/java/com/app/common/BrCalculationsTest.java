package com.app.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;

class BrCalculationsTest {

  @Test
  void ejemploSpec_6sobre10menos2_da75() {
    // Dictadas=10, 6 Presente + 2 Falta + 2 Justificada → 6/(10−2)=75%
    double pct = BrCalculations.attendancePercentage(6, 10, 2, 0);
    assertThat(pct).isCloseTo(75.0, within(0.001));
  }

  @Test
  void exentoTambienDescuentaDenominador() {
    double pct = BrCalculations.attendancePercentage(6, 10, 2, 2);
    assertThat(pct).isCloseTo(100.0, within(0.001)); // 6/(10−2−2)=100%
  }

  @Test
  void sinHorasExigibles_retorna100() {
    assertThat(BrCalculations.attendancePercentage(0, 0, 0, 0)).isEqualTo(100.0);
    assertThat(BrCalculations.attendancePercentage(0, 5, 5, 0)).isEqualTo(100.0);
  }

  @Test
  void semaforo_bordes_50_51_99_100() {
    double margin = BrCalculations.marginHours(100, 0.30); // 30h
    assertThat(margin).isCloseTo(30.0, within(0.001));
    assertThat(BrCalculations.risk(15.0, margin)).isEqualTo(RiskLevel.VERDE); // 50%
    assertThat(BrCalculations.risk(15.3, margin)).isEqualTo(RiskLevel.AMARILLO); // 51%
    assertThat(BrCalculations.risk(29.7, margin)).isEqualTo(RiskLevel.AMARILLO); // 99%
    assertThat(BrCalculations.risk(30.0, margin)).isEqualTo(RiskLevel.ROJO); // 100% → DPI
    assertThat(BrCalculations.risk(45.0, margin)).isEqualTo(RiskLevel.ROJO);
  }

  @Test
  void remaining_nuncaNegativo() {
    assertThat(BrCalculations.remainingHours(10, 30)).isEqualTo(20.0);
    assertThat(BrCalculations.remainingHours(40, 30)).isEqualTo(0.0);
  }
}
