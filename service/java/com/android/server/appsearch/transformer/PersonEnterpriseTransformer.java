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

package com.android.server.appsearch.transformer;

import static android.provider.ContactsContract.AUTHORITY_URI;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.SearchSpec;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.appsearch.contactsindexer.AppSearchHelper;
import com.android.server.appsearch.contactsindexer.appsearchtypes.ContactPoint;
import com.android.server.appsearch.contactsindexer.appsearchtypes.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Contains various transforms for {@link Person} enterprise access.
 */
final class PersonEnterpriseTransformer {
    private static final String TAG = "AppSearchPersonEnterpri";

    // Contacts#CORP_CONTENT_URI is hidden
    private static final Uri CORP_CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI,
            "contacts_corp");

    private static final List<String> PERSON_ACCESSIBLE_PROPERTIES = List.of(
            Person.PERSON_PROPERTY_NAME,
            Person.PERSON_PROPERTY_IMAGE_URI,
            Person.PERSON_PROPERTY_CONTACT_POINTS + "."
                    + ContactPoint.CONTACT_POINT_PROPERTY_LABEL,
            Person.PERSON_PROPERTY_CONTACT_POINTS + "."
                    + ContactPoint.CONTACT_POINT_PROPERTY_EMAIL,
            Person.PERSON_PROPERTY_CONTACT_POINTS + "."
                    + ContactPoint.CONTACT_POINT_PROPERTY_TELEPHONE);

    @VisibleForTesting
    static final Set<String> PERSON_ACCESSIBLE_PROPERTIES_SET = new ArraySet<>(
            PERSON_ACCESSIBLE_PROPERTIES);

    private PersonEnterpriseTransformer() {
    }

    /**
     * Returns whether or not a document of the given package, database, and schema type combination
     * should be transformed for enterprise.
     */
    static boolean shouldTransform(@NonNull String packageName, @NonNull String databaseName,
            @NonNull String schemaType) {
        return schemaType.equals(Person.SCHEMA_TYPE) && packageName.equals("android")
                && databaseName.equals(AppSearchHelper.DATABASE_NAME);
    }

    /**
     * Transforms the imageUri property of a Person document to its corresponding enterprise
     * version.
     * <p>
     * When contacts are accessed through CP2's enterprise uri, CP2 replaces the contact id with an
     * enterprise contact id and the thumbnail uri with a corp thumbnail uri. The enterprise contact
     * id is the original contact id added to an enterprise base id
     * {@link ContactsContract.Contacts#ENTERPRISE_CONTACT_ID_BASE}. The generated corp thumbnail
     * uri includes the original contact id and not the enterprise contact id.
     * <p>
     * In this method, we only transform the imageUri property to the corp thumbnail uri but leave
     * the document id untouched, since changing the document id would interfere with retrieving
     * documents by id.
     */
    @NonNull
    static GenericDocument transformDocument(@NonNull GenericDocument originalDocument) {
        Objects.requireNonNull(originalDocument);
        // Don't set the imageUri property of the document if it's not already set
        if (originalDocument.getPropertyString(Person.PERSON_PROPERTY_IMAGE_URI) == null) {
            return originalDocument;
        }
        GenericDocument.Builder<GenericDocument.Builder<?>> transformedDocumentBuilder =
                new GenericDocument.Builder<>(originalDocument);
        try {
            long originalId = Long.parseLong(originalDocument.getId());
            transformedDocumentBuilder.setPropertyString(Person.PERSON_PROPERTY_IMAGE_URI,
                    getCorpImageUri(originalId));
        } catch (NumberFormatException e) {
            Log.w(TAG, "Failed to set imageUri property", e);
        }
        return transformedDocumentBuilder.build();
    }

    /**
     * Returns the corp thumbnail uri for the given contact id. Note, the generated uri should
     * include the original contact id as opposed to the enterprise contact id returned by CP2 which
     * has {@link Contacts#ENTERPRISE_CONTACT_ID_BASE} added to it.
     */
    @VisibleForTesting
    @NonNull
    static String getCorpImageUri(long contactId) {
        // https://cs.android.com/android/platform/superproject/main/+/main:packages/providers/ContactsProvider/src/com/android/providers/contacts/enterprise/EnterpriseContactsCursorWrapper.java;l=178;drc=a9d2c06a03a653954629ff10070ebbe4ea87d526
        return ContentUris.appendId(CORP_CONTENT_URI.buildUpon(), contactId).appendPath(
                Contacts.Photo.CONTENT_DIRECTORY).build().toString();
    }

    /**
     * Transforms a {@link SearchSpec} through its builder, adding property filters and projections
     * that restrict the allowed properties for the {@link Person} schema type.
     */
    static void transformSearchSpec(@NonNull SearchSpec searchSpec,
            @NonNull SearchSpec.Builder builder) {
        Map<String, List<String>> projections = searchSpec.getProjections();
        Map<String, List<String>> filterProperties = searchSpec.getFilterProperties();
        builder.addProjection(Person.SCHEMA_TYPE,
                getAccessibleProperties(projections.get(Person.SCHEMA_TYPE)));
        builder.addFilterProperties(Person.SCHEMA_TYPE,
                getAccessibleProperties(filterProperties.get(Person.SCHEMA_TYPE)));
    }

    /**
     * Adds allowed properties to the map for each {@link Person} schema type. If properties already
     * exist in the map for {@link Person}, removes the unallowed properties, leaving an
     * intersection of the original properties and the allowed properties.
     */
    static void transformPropertiesMap(@NonNull Map<String, List<String>> propertiesMap) {
        propertiesMap.put(Person.SCHEMA_TYPE,
                getAccessibleProperties(propertiesMap.get(Person.SCHEMA_TYPE)));
    }

    /**
     * If properties is non-null, returns the intersection of properties with the enterprise
     * accessible properties for {@link Person}; otherwise simply returns the enterprise accessible
     * properties for {@link Person}.
     */
    private static List<String> getAccessibleProperties(@Nullable List<String> properties) {
        if (properties == null) {
            return PERSON_ACCESSIBLE_PROPERTIES;
        }
        List<String> filteredProperties = new ArrayList<>();
        for (int i = 0; i < properties.size(); i++) {
            if (PERSON_ACCESSIBLE_PROPERTIES_SET.contains(properties.get(i))) {
                filteredProperties.add(properties.get(i));
            }
        }
        return filteredProperties;
    }
}
