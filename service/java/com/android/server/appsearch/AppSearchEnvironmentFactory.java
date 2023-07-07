package com.android.server.appsearch;

import android.content.Context;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.appsearch.external.localstorage.visibilitystore.VisibilityChecker;
import com.android.server.appsearch.stats.PlatformLogger;
import com.android.server.appsearch.visibilitystore.VisibilityCheckerImpl;

import java.util.concurrent.Executor;

/** This is a factory provider class that holds all factories needed by AppSearch. */
public final class AppSearchEnvironmentFactory {
    private static volatile AppSearchEnvironment mEnvironmentInstance;
    private static volatile AppSearchConfig mConfigInstance;
    private static volatile VisibilityChecker mVisibilityCheckerInstance;
    private static volatile AppSearchInternalLogger mLoggerInstance;

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

    @VisibleForTesting
    static void setEnvironmentInstanceForTest(
            AppSearchEnvironment appSearchEnvironment) {
        synchronized (AppSearchEnvironmentFactory.class) {
            mEnvironmentInstance = appSearchEnvironment;
        }
    }

    @VisibleForTesting
    static void setConfigInstanceForTest(
            AppSearchConfig appSearchConfig) {
        synchronized (AppSearchEnvironmentFactory.class) {
            mConfigInstance = appSearchConfig;
        }
    }

    public static VisibilityChecker getVisibilityCheckerInstance(Context context) {
        VisibilityChecker localRef = mVisibilityCheckerInstance;
        if (localRef == null) {
            synchronized (AppSearchEnvironmentFactory.class) {
                localRef = mVisibilityCheckerInstance;
                if (localRef == null) {
                    mVisibilityCheckerInstance = localRef = new VisibilityCheckerImpl(context);
                }
            }
        }
        return localRef;
    }

    public static AppSearchInternalLogger getLoggerInstance(
            Context context, AppSearchConfig config) {
        AppSearchInternalLogger localRef = mLoggerInstance;
        if (localRef == null) {
            synchronized (AppSearchEnvironmentFactory.class) {
                localRef = mLoggerInstance;
                if (localRef == null) {
                    mLoggerInstance = localRef = new PlatformLogger(context, config);
                }
            }
        }
        return localRef;
    }

    private AppSearchEnvironmentFactory() {}
}

