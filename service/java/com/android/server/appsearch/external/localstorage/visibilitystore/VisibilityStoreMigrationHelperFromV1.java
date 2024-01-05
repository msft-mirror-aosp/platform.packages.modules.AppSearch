/*
 * Copyright 2022 The Android Open Source Project
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

import android.annotation.NonNull;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.PackageIdentifier;
import android.app.appsearch.SetSchemaRequest;
import android.app.appsearch.VisibilityConfig;
import android.app.appsearch.exceptions.AppSearchException;
import android.util.ArraySet;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.appsearch.external.localstorage.AppSearchImpl;
import com.android.server.appsearch.external.localstorage.util.PrefixUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The helper class to store Visibility Document information of version 1 and handle the upgrade to
 * latest version
 *
 * @hide
 */
public class VisibilityStoreMigrationHelperFromV1 {
    private VisibilityStoreMigrationHelperFromV1() {}

    /** Enum in {@link android.app.appsearch.SetSchemaRequest} AppSearch supported role. */
    @VisibleForTesting static final int DEPRECATED_ROLE_HOME = 1;

    /** Enum in {@link android.app.appsearch.SetSchemaRequest} AppSearch supported role. */
    @VisibleForTesting static final int DEPRECATED_ROLE_ASSISTANT = 2;

    /** Reads all stored deprecated Visibility Document in version 0 from icing. */
    static List<VisibilityDocumentV1> getVisibilityDocumentsInVersion1(
            @NonNull AppSearchImpl appSearchImpl) throws AppSearchException {
        List<String> allPrefixedSchemaTypes = appSearchImpl.getAllPrefixedSchemaTypes();
        List<VisibilityDocumentV1> visibilityDocumentV1s =
                new ArrayList<>(allPrefixedSchemaTypes.size());
        for (int i = 0; i < allPrefixedSchemaTypes.size(); i++) {
            String packageName = PrefixUtil.getPackageName(allPrefixedSchemaTypes.get(i));
            if (packageName.equals(VisibilityStore.VISIBILITY_PACKAGE_NAME)) {
                continue; // Our own package. Skip.
            }
            try {
                // Note: We use the prefixed schema type as ids
                visibilityDocumentV1s.add(
                        new VisibilityDocumentV1(
                                appSearchImpl.getDocument(
                                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                                        VisibilityConfig.VISIBILITY_DOCUMENT_NAMESPACE,
                                        allPrefixedSchemaTypes.get(i),
                                        /*typePropertyPaths=*/ Collections.emptyMap())));
            } catch (AppSearchException e) {
                if (e.getResultCode() == AppSearchResult.RESULT_NOT_FOUND) {
                    // TODO(b/172068212): This indicates some desync error. We were expecting a
                    //  document, but didn't find one. Should probably reset AppSearch instead
                    //  of ignoring it.
                    continue;
                }
                // Otherwise, this is some other error we should pass up.
                throw e;
            }
        }
        return visibilityDocumentV1s;
    }

    /**
     * Converts the given list of deprecated Visibility Documents into a Map of {@code
     * <PrefixedSchemaType, VisibilityDocument.Builder of the latest version>}.
     *
     * @param visibilityDocumentV1s The deprecated Visibility Document we found.
     */
    @NonNull
    static List<VisibilityConfig> toVisibilityDocumentsV2(
            @NonNull List<VisibilityDocumentV1> visibilityDocumentV1s) {
        List<VisibilityConfig> latestVisibilityDocuments =
                new ArrayList<>(visibilityDocumentV1s.size());
        for (int i = 0; i < visibilityDocumentV1s.size(); i++) {
            VisibilityDocumentV1 visibilityDocumentV1 = visibilityDocumentV1s.get(i);
            Set<Set<Integer>> visibleToPermissions = new ArraySet<>();
            Set<Integer> deprecatedVisibleToRoles = visibilityDocumentV1.getVisibleToRoles();
            if (deprecatedVisibleToRoles != null) {
                for (int deprecatedVisibleToRole : deprecatedVisibleToRoles) {
                    Set<Integer> visibleToPermission = new ArraySet<>();
                    switch (deprecatedVisibleToRole) {
                        case DEPRECATED_ROLE_HOME:
                            visibleToPermission.add(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA);
                            break;
                        case DEPRECATED_ROLE_ASSISTANT:
                            visibleToPermission.add(
                                    SetSchemaRequest.READ_ASSISTANT_APP_SEARCH_DATA);
                            break;
                    }
                    visibleToPermissions.add(visibleToPermission);
                }
            }
            Set<Integer> deprecatedVisibleToPermissions =
                    visibilityDocumentV1.getVisibleToPermissions();
            if (deprecatedVisibleToPermissions != null) {
                visibleToPermissions.add(deprecatedVisibleToPermissions);
            }

            Set<PackageIdentifier> packageIdentifiers = new ArraySet<>();
            String[] packageNames = visibilityDocumentV1.getPackageNames();
            byte[][] sha256Certs = visibilityDocumentV1.getSha256Certs();
            if (packageNames.length == sha256Certs.length) {
                for (int j = 0; j < packageNames.length; j++) {
                    packageIdentifiers.add(new PackageIdentifier(packageNames[j], sha256Certs[j]));
                }
            }
            VisibilityConfig.Builder latestVisibilityDocumentBuilder =
                    new VisibilityConfig.Builder(visibilityDocumentV1.getId())
                            .setNotDisplayedBySystem(visibilityDocumentV1.isNotDisplayedBySystem())
                            .addVisibleToPackages(packageIdentifiers);
            if (!visibleToPermissions.isEmpty()) {
                latestVisibilityDocumentBuilder.setVisibleToPermissions(visibleToPermissions);
            }
            latestVisibilityDocuments.add(latestVisibilityDocumentBuilder.build());
        }
        return latestVisibilityDocuments;
    }
}
