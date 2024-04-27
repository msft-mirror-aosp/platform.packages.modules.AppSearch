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

package android.app.appsearch;

import com.android.internal.annotations.VisibleForTesting;

/**
 * This is a factory class for implementations needed based on environment for framework code.
 *
 * @hide
 */
public class AppSearchEnvironmentFactory {
    private static volatile AppSearchEnvironment mEnvironmentInstance;

    public static AppSearchEnvironment getEnvironmentInstance() {
        AppSearchEnvironment localRef = mEnvironmentInstance;
        if (localRef == null) {
            synchronized (AppSearchEnvironmentFactory.class) {
                localRef = mEnvironmentInstance;
                if (localRef == null) {
                    mEnvironmentInstance = localRef = new FrameworkAppSearchEnvironment();
                }
            }
        }
        return localRef;
    }

    @VisibleForTesting
    public static void setEnvironmentInstanceForTest(AppSearchEnvironment appSearchEnvironment) {
        synchronized (AppSearchEnvironmentFactory.class) {
            mEnvironmentInstance = appSearchEnvironment;
        }
    }

    private AppSearchEnvironmentFactory() {}
}
