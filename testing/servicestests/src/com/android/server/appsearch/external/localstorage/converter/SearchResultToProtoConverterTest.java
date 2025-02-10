/*
 * Copyright 2021 The Android Open Source Project
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

package com.android.server.appsearch.external.localstorage.converter;

import static com.android.server.appsearch.external.localstorage.util.PrefixUtil.removePrefixesFromDocument;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.app.appsearch.SearchResult;
import android.app.appsearch.SearchResultPage;
import android.app.appsearch.exceptions.AppSearchException;

import com.android.appsearch.flags.Flags;
import com.android.server.appsearch.external.localstorage.AppSearchConfigImpl;
import com.android.server.appsearch.external.localstorage.LocalStorageIcingOptionsConfig;
import com.android.server.appsearch.external.localstorage.SchemaCache;
import com.android.server.appsearch.external.localstorage.UnlimitedLimitConfig;
import com.android.server.appsearch.external.localstorage.util.PrefixUtil;
import com.android.server.appsearch.icing.proto.DocumentProto;
import com.android.server.appsearch.icing.proto.SchemaTypeConfigProto;
import com.android.server.appsearch.icing.proto.SearchResultProto;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Map;

public class SearchResultToProtoConverterTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testToSearchResultProto() throws Exception {
        final String prefix =
                "com.package.foo"
                        + PrefixUtil.PACKAGE_DELIMITER
                        + "databaseName"
                        + PrefixUtil.DATABASE_DELIMITER;
        final String id = "id";
        final String namespace = prefix + "namespace";
        String schemaType = "schema";
        String parentSchemaType = "parentSchema";
        final String prefixedSchemaType = prefix + schemaType;
        final String prefixedParentSchemaType = prefix + parentSchemaType;
        final AppSearchConfigImpl config =
                new AppSearchConfigImpl(
                        new UnlimitedLimitConfig(),
                        new LocalStorageIcingOptionsConfig(),
                        /* storeParentInfoAsSyntheticProperty= */ false,
                        /* shouldRetrieveParentInfo= */ true);

        // Building the SearchResult received from query.
        DocumentProto.Builder documentProtoBuilder =
                DocumentProto.newBuilder()
                        .setUri(id)
                        .setNamespace(namespace)
                        .setSchema(prefixedSchemaType);

        // A joined document
        DocumentProto.Builder joinedDocProtoBuilder =
                DocumentProto.newBuilder()
                        .setUri("id2")
                        .setNamespace(namespace)
                        .setSchema(prefixedSchemaType);

        SearchResultProto.ResultProto joinedResultProto =
                SearchResultProto.ResultProto.newBuilder()
                        .setDocument(joinedDocProtoBuilder)
                        .build();

        SearchResultProto.ResultProto resultProto =
                SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProtoBuilder)
                        .addJoinedResults(joinedResultProto)
                        .build();

        SearchResultProto searchResultProto =
                SearchResultProto.newBuilder().addResults(resultProto).build();

        SchemaTypeConfigProto parentSchemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().setSchemaType(prefixedParentSchemaType).build();
        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder()
                        .addParentTypes(prefixedParentSchemaType)
                        .setSchemaType(prefixedSchemaType)
                        .build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap =
                ImmutableMap.of(
                        prefix,
                        ImmutableMap.of(
                                prefixedSchemaType, schemaTypeConfigProto,
                                prefixedParentSchemaType, parentSchemaTypeConfigProto));
        SchemaCache schemaCache = new SchemaCache(schemaMap);

        removePrefixesFromDocument(documentProtoBuilder);
        removePrefixesFromDocument(joinedDocProtoBuilder);
        SearchResultPage searchResultPage =
                SearchResultToProtoConverter.toSearchResultPage(
                        searchResultProto, schemaCache, config);
        assertThat(searchResultPage.getResults()).hasSize(1);
        SearchResult result = searchResultPage.getResults().get(0);
        assertThat(result.getPackageName()).isEqualTo("com.package.foo");
        assertThat(result.getDatabaseName()).isEqualTo("databaseName");
        assertThat(result.getGenericDocument())
                .isEqualTo(
                        GenericDocumentToProtoConverter.toGenericDocument(
                                documentProtoBuilder.build(), prefix, schemaCache, config));

        assertThat(result.getJoinedResults()).hasSize(1);
        assertThat(result.getJoinedResults().get(0).getGenericDocument())
                .isEqualTo(
                        GenericDocumentToProtoConverter.toGenericDocument(
                                joinedDocProtoBuilder.build(), prefix, schemaCache, config));

        if (Flags.enableSearchResultParentTypes()) {
            assertThat(result.getParentTypeMap())
                    .isEqualTo(ImmutableMap.of(schemaType, ImmutableList.of(parentSchemaType)));
        } else {
            assertThat(result.getParentTypeMap()).isEmpty();
        }
    }

    @Test
    public void testToSearchResultProtoWithDoublyNested() throws Exception {
        final String prefix =
                "com.package.foo"
                        + PrefixUtil.PACKAGE_DELIMITER
                        + "databaseName"
                        + PrefixUtil.DATABASE_DELIMITER;
        final String id = "id";
        final String namespace = prefix + "namespace";
        final String schemaType = prefix + "schema";

        // Building the SearchResult received from query.
        DocumentProto.Builder documentProtoBuilder =
                DocumentProto.newBuilder().setUri(id).setNamespace(namespace).setSchema(schemaType);

        // A joined document
        DocumentProto.Builder joinedDocProtoBuilder =
                DocumentProto.newBuilder()
                        .setUri("id2")
                        .setNamespace(namespace)
                        .setSchema(schemaType);

        SearchResultProto.ResultProto joinedResultProto =
                SearchResultProto.ResultProto.newBuilder()
                        .setDocument(joinedDocProtoBuilder)
                        .build();

        SearchResultProto.ResultProto nestedJoinedResultProto =
                SearchResultProto.ResultProto.newBuilder()
                        .setDocument(joinedDocProtoBuilder)
                        .addJoinedResults(joinedResultProto)
                        .build();

        SearchResultProto.ResultProto resultProto =
                SearchResultProto.ResultProto.newBuilder()
                        .setDocument(documentProtoBuilder)
                        .addJoinedResults(nestedJoinedResultProto)
                        .build();

        SearchResultProto searchResultProto =
                SearchResultProto.newBuilder().addResults(resultProto).build();

        SchemaTypeConfigProto schemaTypeConfigProto =
                SchemaTypeConfigProto.newBuilder().setSchemaType(schemaType).build();
        Map<String, Map<String, SchemaTypeConfigProto>> schemaMap =
                ImmutableMap.of(prefix, ImmutableMap.of(schemaType, schemaTypeConfigProto));

        removePrefixesFromDocument(documentProtoBuilder);
        Exception e =
                assertThrows(
                        AppSearchException.class,
                        () ->
                                SearchResultToProtoConverter.toSearchResultPage(
                                        searchResultProto,
                                        new SchemaCache(schemaMap),
                                        new AppSearchConfigImpl(
                                                new UnlimitedLimitConfig(),
                                                new LocalStorageIcingOptionsConfig())));
        assertThat(e.getMessage())
                .isEqualTo("Nesting joined results within joined results not allowed.");
    }
}
