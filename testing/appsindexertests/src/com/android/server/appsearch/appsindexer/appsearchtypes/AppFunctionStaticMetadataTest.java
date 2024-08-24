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

package com.android.server.appsearch.appsindexer.appsearchtypes;

import static com.google.common.truth.Truth.assertThat;

import android.app.appsearch.AppSearchSchema;

import org.junit.Test;

public class AppFunctionStaticMetadataTest {
    @Test
    public void testAppFunction() {
        String functionId = "com.example.message#send_message";
        String schemaName = "send_message";
        String schemaCategory = "messaging";
        int stringResId = 3;
        long schemaVersion = 2;
        boolean enabledByDefault = false;
        boolean restrictCallersWithExecuteAppFunctions = false;
        String packageName = "com.example.message";

        AppFunctionStaticMetadata appFunction =
                new AppFunctionStaticMetadata.Builder(packageName, functionId, "android")
                        .setSchemaName(schemaName)
                        .setSchemaVersion(schemaVersion)
                        .setSchemaCategory(schemaCategory)
                        .setEnabledByDefault(enabledByDefault)
                        .setRestrictCallersWithExecuteAppFunctions(
                                restrictCallersWithExecuteAppFunctions)
                        .setDisplayNameStringRes(stringResId)
                        .build();
        assertThat(appFunction.getFunctionId()).isEqualTo(functionId);
        assertThat(appFunction.getPackageName()).isEqualTo(packageName);
        assertThat(appFunction.getSchemaName()).isEqualTo(schemaName);
        assertThat(appFunction.getSchemaVersion()).isEqualTo(schemaVersion);
        assertThat(appFunction.getRestrictCallersWithExecuteAppFunctions())
                .isEqualTo(restrictCallersWithExecuteAppFunctions);
        assertThat(appFunction.getSchemaCategory()).isEqualTo(schemaCategory);
        assertThat(appFunction.getEnabledByDefault()).isEqualTo(enabledByDefault);
        assertThat(appFunction.getDisplayNameStringRes()).isEqualTo(stringResId);
        assertThat(appFunction.getMobileApplicationQualifiedId())
                .isEqualTo("android$apps-db/apps#com.example.message");
    }

    @Test
    public void testSchemaName() {
        String packageName = "com.example.message";
        String schemaName = AppFunctionStaticMetadata.getSchemaNameForPackage(packageName);
        assertThat(schemaName).isEqualTo("AppFunctionStaticMetadata-com.example.message");
    }

    @Test
    public void testChildSchema() {
        AppSearchSchema appSearchSchema =
                AppFunctionStaticMetadata.createAppFunctionSchemaForPackage("com.xyz");

        if (AppFunctionStaticMetadata.shouldSetParentType()) {
            assertThat(appSearchSchema.getParentTypes())
                    .containsExactly(AppFunctionStaticMetadata.SCHEMA_TYPE);
        }
    }

    @Test
    public void testParentSchema() {
        assertThat(AppFunctionStaticMetadata.PARENT_TYPE_APPSEARCH_SCHEMA.getSchemaType())
                .isEqualTo(AppFunctionStaticMetadata.SCHEMA_TYPE);
    }
}
