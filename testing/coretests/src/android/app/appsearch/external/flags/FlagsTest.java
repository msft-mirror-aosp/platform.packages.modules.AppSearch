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

package android.app.appsearch.flags;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class FlagsTest {
    @Test
    public void testFlagValue_enableSafeParcelable2() {
        assertThat(Flags.FLAG_ENABLE_SAFE_PARCELABLE_2)
                .isEqualTo("com.android.appsearch.flags.enable_safe_parcelable_2");
    }

    @Test
    public void testFlagValue_enableListFilterHasPropertyFunction() {
        assertThat(Flags.FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION)
                .isEqualTo("com.android.appsearch.flags.enable_list_filter_has_property_function");
    }

    @Test
    public void testFlagValue_enableGroupingTypePerSchema() {
        assertThat(Flags.FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA)
                .isEqualTo("com.android.appsearch.flags.enable_grouping_type_per_schema");
    }

    @Test
    public void testFlagValue_enableGenericDocumentCopyConstructor() {
        assertThat(Flags.FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR)
                .isEqualTo(
                        "com.android"
                                + ".appsearch.flags.enable_generic_document_copy_constructor");
    }

    @Test
    public void testFlagValue_enableSearchSpecFilterProperties() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_FILTER_PROPERTIES)
                .isEqualTo("com.android.appsearch.flags.enable_search_spec_filter_properties");
    }

    @Test
    public void testFlagValue_enableSearchSpecSetSearchSourceLogTag() {
        assertThat(Flags.FLAG_ENABLE_SEARCH_SPEC_SET_SEARCH_SOURCE_LOG_TAG)
                .isEqualTo(
                        "com.android.appsearch.flags.enable_search_spec_set_search_source_log_tag");
    }

    @Test
    public void testFlagValue_enablePutDocumentsRequestAddTakenActions() {
        assertThat(Flags.FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS)
                .isEqualTo(
                        "com.android.appsearch.flags.enable_put_documents_request_add_taken_actions");
    }
}
