package com.android.server.appsearch;

/** This is a factory class that returns implementation for AppSearchEnvironment. */
public class AppSearchEnvironmentFactory {

  private static volatile AppSearchEnvironment mInstance;

  public static AppSearchEnvironment getInstance() {
    AppSearchEnvironment localRef = mInstance;
    if (localRef == null) {
      synchronized (AppSearchEnvironmentFactory.class) {
        localRef = mInstance;
        if (localRef == null) {
            mInstance = localRef = new FrameworkAppSearchEnvironment();
        }
      }
    }
    return localRef;
  }

  private AppSearchEnvironmentFactory() {}
}

