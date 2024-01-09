/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.server.appsearch.external.localstorage.visibilitystore;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.appsearch.AppSearchSchema;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.PackageIdentifier;
import android.app.appsearch.SetSchemaRequest;
import android.app.appsearch.VisibilityConfig;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class VisibilityConfigTest {

    @Test
    public void testVisibilityConfigInitialization() {
        // Create a VisibilityDocument for testing
        GenericDocument visibilityDocument =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "", "someSchema", "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBoolean("notPlatformSurfaceable", false)
                        .setPropertyString("packageName", "")
                        .setPropertyBytes("sha256Cert", new byte[32])
                        .build();

        // Create a VisibilityOverlay for testing
        GenericDocument visibilityOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "overlay", "someSchema", "PublicAclOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyString("publiclyVisibleTargetPackage", "com.example.test")
                        .setPropertyBytes("publiclyVisibleTargetPackageSha256Cert", new byte[32])
                        .build();

        // Create a VisibilityConfig using the Builder
        VisibilityConfig visibilityConfig =
                VisibilityConfig.createVisibilityConfig(visibilityDocument, visibilityOverlay);

        // Check if the properties are set correctly
        assertEquals(visibilityDocument, visibilityConfig.createVisibilityDocument());
        assertEquals(visibilityOverlay, visibilityConfig.createPublicAclOverlay());
    }

    @Test
    public void testToGenericDocuments() {
        // Create a SetSchemaRequest for testing
        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder()
                        .addSchemas(new AppSearchSchema.Builder("someSchema").build())
                        .setPubliclyVisibleSchema(
                                "someSchema",
                                new PackageIdentifier("com.example.test", new byte[32]))
                        .build();

        // Convert the SetSchemaRequest to a list of VisibilityConfig
        List<VisibilityConfig> visibilityConfigs =
                VisibilityConfig.toVisibilityConfigs(setSchemaRequest);

        GenericDocument expectedDocument =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "", "someSchema", "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBoolean("notPlatformSurfaceable", false)
                        .setPropertyString("packageName")
                        .setPropertyBytes("sha256Cert")
                        .build();

        // Create a VisibilityOverlay for testing
        GenericDocument expectedOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "overlay", "someSchema", "PublicAclOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyString("publiclyVisibleTargetPackage", "com.example.test")
                        .setPropertyBytes("publiclyVisibleTargetPackageSha256Cert", new byte[32])
                        .build();

        // Check if the conversion is correct
        assertEquals(1, visibilityConfigs.size());
        VisibilityConfig visibilityConfig = visibilityConfigs.get(0);

        assertEquals(visibilityConfig.createVisibilityDocument(), expectedDocument);
        assertEquals(visibilityConfig.createPublicAclOverlay(), expectedOverlay);
    }

    @Test
    public void testVisibilityConfigEquals() {
        // Create two VisibilityConfig instances with the same properties
        GenericDocument visibilityDocument =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "namespace", "id1", "visibilityDocument")
                        .setPropertyString("packageName", "")
                        .setPropertyBytes("sha256Cert", new byte[32])
                        .build();

        // Create a VisibilityOverlay for testing
        GenericDocument visibilityOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "namespace", "id1", "visibilityDocumentOverlay")
                        .setPropertyString("stringKey1", "com.example.test")
                        .setPropertyBytes("byteKey1", new byte[32])
                        .build();

        VisibilityConfig param1 =
                VisibilityConfig.createVisibilityConfig(visibilityDocument, visibilityOverlay);
        VisibilityConfig param2 =
                VisibilityConfig.createVisibilityConfig(visibilityDocument, visibilityOverlay);

        // Test equals method
        assertTrue(param1.equals(param2));
        assertTrue(param2.equals(param1));
    }

    @Test
    public void testToVisibilityConfig_publicAcl() {
        byte[] packageSha256Cert = new byte[32];
        packageSha256Cert[0] = 24;
        packageSha256Cert[8] = 23;
        packageSha256Cert[16] = 22;
        packageSha256Cert[24] = 21;

        // Create a SetSchemaRequest for testing
        SetSchemaRequest setSchemaRequest =
                new SetSchemaRequest.Builder()
                        .addSchemas(new AppSearchSchema.Builder("testSchema").build())
                        .setPubliclyVisibleSchema(
                                "testSchema",
                                new PackageIdentifier("com.example.test", packageSha256Cert))
                        .build();

        // Convert the SetSchemaRequest to GenericDocument map
        List<VisibilityConfig> visibilityConfigs =
                VisibilityConfig.toVisibilityConfigs(setSchemaRequest);

        // Check if the conversion is correct
        assertThat(visibilityConfigs).hasSize(1);
        VisibilityConfig visibilityConfig = visibilityConfigs.get(0);
        assertEquals(visibilityConfig.getSchemaType(), "testSchema");
        assertNotNull(visibilityConfig.getPubliclyVisibleTargetPackage());
        assertEquals(
                "com.example.test",
                visibilityConfig.getPubliclyVisibleTargetPackage().getPackageName());
        assertEquals(
                packageSha256Cert,
                visibilityConfig.getPubliclyVisibleTargetPackage().getSha256Certificate());
    }

    @Test
    public void testVisibilityConfig_rebuild() {
        String visibleToPackage = "com.example.package";
        byte[] visibleToPackageCert = new byte[32];

        String publiclyVisibleTarget = "com.example.test";
        byte[] publiclyVisibleTargetCert = new byte[32];

        // Create a VisibilityDocument for testing
        GenericDocument visibilityDocument =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "", "someSchema", "VisibilityType")
                        .setCreationTimestampMillis(0)
                        .setPropertyBoolean("notPlatformSurfaceable", false)
                        .setPropertyString("packageName", visibleToPackage)
                        .setPropertyBytes("sha256Cert", visibleToPackageCert)
                        .build();

        // Create a VisibilityOverlay for testing
        GenericDocument visibilityOverlay =
                new GenericDocument.Builder<GenericDocument.Builder<?>>(
                                "overlay", "someSchema", "PublicAclOverlayType")
                        .setCreationTimestampMillis(0)
                        .setPropertyString("publiclyVisibleTargetPackage", "com.example.test")
                        .setPropertyBytes("publiclyVisibleTargetPackageSha256Cert", new byte[32])
                        .build();

        // Create a VisibilityConfig using the Builder
        VisibilityConfig visibilityConfig =
                VisibilityConfig.createVisibilityConfig(visibilityDocument, visibilityOverlay);

        // Check if the properties are set correctly
        assertEquals(visibilityDocument, visibilityConfig.createVisibilityDocument());
        assertEquals(visibilityOverlay, visibilityConfig.createPublicAclOverlay());

        VisibilityConfig.Builder builder = new VisibilityConfig.Builder(visibilityConfig);

        VisibilityConfig rebuild = builder.build();
        assertTrue(visibilityConfig.equals(rebuild));

        VisibilityConfig modifiedConfig =
                builder.setSchemaType("prefixedSchema")
                        .setNotDisplayedBySystem(true)
                        .setVisibleToPermissions(
                                ImmutableSet.of(
                                        ImmutableSet.of(
                                                SetSchemaRequest.READ_SMS,
                                                SetSchemaRequest.READ_CALENDAR)))
                        .clearVisibleToPackages()
                        .addVisibleToPackage(
                                new PackageIdentifier("com.example.other", new byte[32]))
                        .setPubliclyVisibleTargetPackage(
                                new PackageIdentifier("com.example.other", new byte[32]))
                        .build();
        assertEquals(modifiedConfig.getSchemaType(), "prefixedSchema");

        // Check that the rebuild stayed the same
        assertEquals(rebuild.getSchemaType(), "someSchema");
        assertFalse(rebuild.isNotDisplayedBySystem());
        assertEquals(rebuild.getVisibleToPermissions(), Collections.emptySet());
        assertEquals(
                rebuild.getVisibleToPackages(),
                ImmutableList.of(new PackageIdentifier(visibleToPackage, visibleToPackageCert)));
        assertEquals(
                rebuild.getPubliclyVisibleTargetPackage(),
                new PackageIdentifier(publiclyVisibleTarget, publiclyVisibleTargetCert));
    }
}
