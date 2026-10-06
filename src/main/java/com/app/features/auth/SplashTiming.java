package com.app.features.auth;

/** Small timing calculation used to keep splash timing independent of animation APIs. */
public final class SplashTiming {

  private SplashTiming() {}

  public static long remainingMillis(long startMillis, long nowMillis, long minMillis) {
    return Math.max(0, minMillis - (nowMillis - startMillis));
  }
}
