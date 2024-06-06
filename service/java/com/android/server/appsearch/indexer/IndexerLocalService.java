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

package com.android.server.appsearch.indexer;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.CancellationSignal;
import android.os.UserHandle;

import com.android.server.LocalManagerRegistry;

/**
 * An interface for Indexers local services.
 *
 * @see LocalManagerRegistry#addManager
 */
public interface IndexerLocalService {
    /** Runs a scheduled update for the user specified by userHandle. */
    void doUpdateForUser(@NonNull UserHandle userHandle, @Nullable CancellationSignal signal);
}