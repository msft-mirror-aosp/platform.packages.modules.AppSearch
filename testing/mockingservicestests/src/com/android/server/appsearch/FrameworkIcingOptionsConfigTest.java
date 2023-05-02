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

import static com.google.common.truth.Truth.assertThat;

import android.provider.DeviceConfig;

import com.android.modules.utils.testing.TestableDeviceConfig;
import com.android.server.appsearch.external.localstorage.IcingOptionsConfig;

import org.junit.Rule;
import org.junit.Test;

public class FrameworkIcingOptionsConfigTest {
    @Rule
    public final TestableDeviceConfig.TestableDeviceConfigRule
            mDeviceConfigRule = new TestableDeviceConfig.TestableDeviceConfigRule();

    @Test
    public void testDefaultValues() {
        FrameworkIcingOptionsConfig icingOptionsConfig = new FrameworkIcingOptionsConfig();

        assertThat(icingOptionsConfig.getMaxTokenLength()).isEqualTo(
                IcingOptionsConfig.DEFAULT_MAX_TOKEN_LENGTH);
        assertThat(icingOptionsConfig.getIndexMergeSize()).isEqualTo(
                IcingOptionsConfig.DEFAULT_INDEX_MERGE_SIZE);
        assertThat(icingOptionsConfig.getDocumentStoreNamespaceIdFingerprint()).isEqualTo(
                IcingOptionsConfig.DEFAULT_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT);
        assertThat(icingOptionsConfig.getOptimizeRebuildIndexThreshold()).isEqualTo(
                IcingOptionsConfig.DEFAULT_OPTIMIZE_REBUILD_INDEX_THRESHOLD);
        assertThat(icingOptionsConfig.getCompressionLevel()).isEqualTo(
                IcingOptionsConfig.DEFAULT_COMPRESSION_LEVEL);
    }

    @Test
    public void testCustomizedValues() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_APPSEARCH,
                FrameworkIcingOptionsConfig.KEY_ICING_MAX_TOKEN_LENGTH,
                Integer.toString(IcingOptionsConfig.DEFAULT_MAX_TOKEN_LENGTH + 1),
                false);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_APPSEARCH,
                FrameworkIcingOptionsConfig.KEY_ICING_INDEX_MERGE_SIZE,
                Integer.toString(IcingOptionsConfig.DEFAULT_INDEX_MERGE_SIZE + 1),
                false);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_APPSEARCH,
                FrameworkIcingOptionsConfig.KEY_ICING_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT,
                Boolean.toString(
                        !IcingOptionsConfig.DEFAULT_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT),
                false);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_APPSEARCH,
                FrameworkIcingOptionsConfig.KEY_ICING_OPTIMIZE_REBUILD_INDEX_THRESHOLD,
                Float.toString(IcingOptionsConfig.DEFAULT_OPTIMIZE_REBUILD_INDEX_THRESHOLD + 1.0f),
                false);
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_APPSEARCH,
                FrameworkIcingOptionsConfig.KEY_ICING_COMPRESSION_LEVEL,
                Integer.toString(IcingOptionsConfig.DEFAULT_COMPRESSION_LEVEL + 1),
                false);


        FrameworkIcingOptionsConfig icingOptionsConfig = new FrameworkIcingOptionsConfig();

        assertThat(icingOptionsConfig.getMaxTokenLength()).isEqualTo(
                IcingOptionsConfig.DEFAULT_MAX_TOKEN_LENGTH + 1);
        assertThat(icingOptionsConfig.getIndexMergeSize()).isEqualTo(
                IcingOptionsConfig.DEFAULT_INDEX_MERGE_SIZE + 1);
        assertThat(icingOptionsConfig.getDocumentStoreNamespaceIdFingerprint()).isEqualTo(
                !IcingOptionsConfig.DEFAULT_DOCUMENT_STORE_NAMESPACE_ID_FINGERPRINT);
        assertThat(icingOptionsConfig.getOptimizeRebuildIndexThreshold()).isEqualTo(
                IcingOptionsConfig.DEFAULT_OPTIMIZE_REBUILD_INDEX_THRESHOLD + 1.0f);
        assertThat(icingOptionsConfig.getCompressionLevel()).isEqualTo(
                IcingOptionsConfig.DEFAULT_COMPRESSION_LEVEL + 1);
    }
}
