/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.appsearch;

import com.android.internal.annotations.VisibleForTesting;

import java.util.concurrent.Executor;

/** This is a factory class for implementations needed based on environment for service code. */
public final class AppSearchConfigFactory {
    private static volatile FrameworkAppSearchConfig mConfigInstance;

    public static FrameworkAppSearchConfig getConfigInstance(Executor executor) {
        FrameworkAppSearchConfig localRef = mConfigInstance;
        if (localRef == null) {
            synchronized (AppSearchConfigFactory.class) {
                localRef = mConfigInstance;
                if (localRef == null) {
                    mConfigInstance = localRef = FrameworkAppSearchConfigImpl
                            .getInstance(executor);
                }
            }
        }
        return localRef;
    }

    @VisibleForTesting
    static void setConfigInstanceForTest(
            FrameworkAppSearchConfig appSearchConfig) {
        synchronized (AppSearchConfigFactory.class) {
            mConfigInstance = appSearchConfig;
        }
    }

    private AppSearchConfigFactory() {
    }
}
