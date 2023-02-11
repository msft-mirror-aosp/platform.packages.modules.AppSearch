package com.android.server.appsearch;

import java.util.concurrent.Executor;

/** This is a factory provider class that holds all factories needed by AppSearch. */
public final class AppSearchEnvironmentFactory {
    private static volatile AppSearchEnvironment mEnvironmentInstance;
    private static volatile AppSearchConfig mConfigInstance;

    public static AppSearchEnvironment getEnvironmentInstance() {
      AppSearchEnvironment localRef = mEnvironmentInstance;
      if (localRef == null) {
        synchronized (AppSearchEnvironmentFactory.class) {
          localRef = mEnvironmentInstance;
          if (localRef == null) {
              mEnvironmentInstance = localRef =
                      new FrameworkAppSearchEnvironment();
          }
        }
      }
      return localRef;
    }

    public static AppSearchConfig getConfigInstance(Executor executor) {
      AppSearchConfig localRef = mConfigInstance;
      if (localRef == null) {
        synchronized (AppSearchEnvironmentFactory.class) {
          localRef = mConfigInstance;
          if (localRef == null) {
              mConfigInstance = localRef = FrameworkAppSearchConfig
                              .getInstance(executor);
          }
        }
      }
      return localRef;
    }

    private AppSearchEnvironmentFactory() {}
}

