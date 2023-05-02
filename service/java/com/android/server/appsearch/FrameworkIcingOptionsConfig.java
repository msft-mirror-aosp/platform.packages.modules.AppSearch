/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.provider.DeviceConfig;

import com.android.server.appsearch.external.localstorage.IcingOptionsConfig;

/**
 * Implementation of {@link IcingOptionsConfig} using {@link DeviceConfig}.
 *
 * @hide
 */
class FrameworkIcingOptionsConfig implements IcingOptionsConfig {
    static final String KEY_ICING_MAX_TOKEN_LENGTH = "icing_max_token_length";
    static final String KEY_ICING_INDEX_MERGE_SIZE = "icing_index_merge_size";
    static final String KEY_ICING_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT =
            "icing_document_store_namespace_id_fingerprint";
    static final String KEY_ICING_OPTIMIZE_REBUILD_INDEX_THRESHOLD =
            "icing_optimize_rebuild_index_threshold";
    static final String KEY_ICING_COMPRESSION_LEVEL = "icing_compression_level";

    @Override
    public int getMaxTokenLength() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_APPSEARCH, KEY_ICING_MAX_TOKEN_LENGTH,
                DEFAULT_MAX_TOKEN_LENGTH);
    }

    @Override
    public int getIndexMergeSize() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_APPSEARCH, KEY_ICING_INDEX_MERGE_SIZE,
                DEFAULT_INDEX_MERGE_SIZE);
    }

    @Override
    public boolean getDocumentStoreNamespaceIdFingerprint() {
        return DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_APPSEARCH,
                KEY_ICING_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT,
                DEFAULT_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT);
    }

    @Override
    public float getOptimizeRebuildIndexThreshold() {
        return DeviceConfig.getFloat(DeviceConfig.NAMESPACE_APPSEARCH,
                KEY_ICING_OPTIMIZE_REBUILD_INDEX_THRESHOLD,
                DEFAULT_OPTIMIZE_REBUILD_INDEX_THRESHOLD);
    }

    @Override
    public int getCompressionLevel() {
        return DeviceConfig.getInt(DeviceConfig.NAMESPACE_APPSEARCH, KEY_ICING_COMPRESSION_LEVEL,
                DEFAULT_COMPRESSION_LEVEL);
    }
}
