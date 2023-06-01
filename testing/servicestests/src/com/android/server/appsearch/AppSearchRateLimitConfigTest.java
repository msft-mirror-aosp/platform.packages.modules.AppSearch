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

import com.android.server.appsearch.external.localstorage.stats.CallStats;

import org.junit.Test;

public class AppSearchRateLimitConfigTest {
    @Test
    public void testCustomRateLimitConfig() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments:5;localGetDocuments:11;localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(5);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(11);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testApiCostStringCaseSensitive() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "putdocuments:5;LOCALgEtDOcUmENts:11;SETSCHEMA:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        // Unable to set API costs since the API name is case-sensitive
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_unspecifiedApiCosts() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments;localGetDocuments:;localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_nonIntegerApiCosts() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments:0.5;localGetDocuments:11;localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(11);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);

        rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments:cost");
        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_negativeApiCostNotAllowed() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments:-1;localGetDocuments:11;localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(11);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_emptyString() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f, "");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_emptySegments() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f, ";localPutDocuments:10;");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(10);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_wrongCostDelimiter() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f, "localPutDocuments;10");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_wrongEntryDelimiter() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "localPutDocuments:10:localGetDocuments:11:localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(1);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testInvalidApiCostsString_invalidApiEntry() {
        AppSearchRateLimitConfig rateLimitConfig =
                AppSearchRateLimitConfig.create(1000, 0.8f,
                        "foo:10;localGetDocuments:11;localSetSchema:99");

        assertThat(rateLimitConfig.getTaskQueueTotalCapacity()).isEqualTo(1000);
        assertThat(rateLimitConfig.getTaskQueuePerPackageCapacity()).isEqualTo(800);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(11);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);
        // Unset API costs = 1 by default
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_NAMESPACES)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_STORAGE_INFO)).isEqualTo(1);
        assertThat(rateLimitConfig.getApiCost(
                CallStats.CALL_TYPE_REMOVE_DOCUMENT_BY_SEARCH)).isEqualTo(1);
    }
}