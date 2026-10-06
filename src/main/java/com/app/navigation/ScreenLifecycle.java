package com.app.navigation;

/** Optional cleanup hook for a screen that is removed from the router. */
public interface ScreenLifecycle {

  void onHidden();
}
