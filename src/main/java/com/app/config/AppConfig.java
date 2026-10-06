package com.app.config;

/** Configuración central. Overrides vía -Djavapp.* o env. */
public final class AppConfig {

  public static final String APP_NAME = "JavappDesktop";
  public static final int INACTIVITY_MINUTES_DEFAULT = 15;
  public static final double MAX_FALTAS_DEFAULT = 0.30;
  public static final long SPLASH_MIN_MILLIS_DEFAULT = 5000;

  private AppConfig() {}

  public static int inactivityMinutes() {
    String v =
        System.getProperty(
            "javapp.inactivityMinutes",
            System.getenv()
                .getOrDefault(
                    "JAVAPP_INACTIVITY_MINUTES", String.valueOf(INACTIVITY_MINUTES_DEFAULT)));
    try {
      return Math.max(1, Integer.parseInt(v.trim()));
    } catch (NumberFormatException e) {
      return INACTIVITY_MINUTES_DEFAULT;
    }
  }

  public static double maxFaltasRatio() {
    String v =
        System.getProperty(
            "javapp.maxFaltas",
            System.getenv().getOrDefault("JAVAPP_MAX_FALTAS", String.valueOf(MAX_FALTAS_DEFAULT)));
    try {
      double d = Double.parseDouble(v.trim());
      return (d > 0 && d < 1) ? d : MAX_FALTAS_DEFAULT;
    } catch (NumberFormatException e) {
      return MAX_FALTAS_DEFAULT;
    }
  }

  /** Base URL del backend real (fase mock: no se usa, reservado para HttpApiClient). */
  public static String baseUrl() {
    return System.getProperty(
        "javapp.baseUrl",
        System.getenv().getOrDefault("JAVAPP_BASE_URL", "http://localhost:8080/api"));
  }

  /** Tiempo mínimo visible del Splash (ms) para que la bienvenida se perciba. */
  public static long splashMinMillis() {
    String v =
        System.getProperty(
            "javapp.splashMinMillis",
            System.getenv()
                .getOrDefault(
                    "JAVAPP_SPLASH_MIN_MILLIS", String.valueOf(SPLASH_MIN_MILLIS_DEFAULT)));
    try {
      return Math.max(0, Long.parseLong(v.trim()));
    } catch (NumberFormatException e) {
      return SPLASH_MIN_MILLIS_DEFAULT;
    }
  }
}
