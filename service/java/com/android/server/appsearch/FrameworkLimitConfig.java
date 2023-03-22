/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.annotation.NonNull;

import com.android.server.appsearch.external.localstorage.LimitConfig;

import java.util.Objects;

class FrameworkLimitConfig implements LimitConfig {
    private final AppSearchConfig mAppSearchConfig;

    FrameworkLimitConfig(@NonNull AppSearchConfig appSearchConfig) {
        mAppSearchConfig = Objects.requireNonNull(appSearchConfig);
    }

    @Override
    public int getMaxDocumentSizeBytes() {
        return mAppSearchConfig.getCachedLimitConfigMaxDocumentSizeBytes();
    }

    @Override
    public int getMaxDocumentCount() {
        return mAppSearchConfig.getCachedLimitConfigMaxDocumentCount();
    }

    @Override
    public int getMaxSuggestionCount() {
        return mAppSearchConfig.getCachedLimitConfigMaxSuggestionCount();
    }

    @Override
    public boolean getDocumentStoreNamespaceIdFingerprint() {
        return false;   // Off by default. Populate with flag value in followup
    }

    @Override
    public float getOptimizeRebuildIndexThreshold() {
        return 0;  // Off by default. Populate with flag value in followup
    }
}
