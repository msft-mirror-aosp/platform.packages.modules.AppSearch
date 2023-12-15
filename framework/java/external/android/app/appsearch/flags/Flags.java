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


/**
 * Flags to control different features.
 *
 * <p>In Jetpack, those values can't be changed during runtime.
 *
 * @hide
 */
public final class Flags {
    private Flags() {}

    // The prefix of all the flags defined for AppSearch. The prefix has
    // "com.android.appsearch.flags", aka the package name for generated AppSearch flag classes in
    // the framework, plus an additional trailing '.'.
    private static final String FLAG_PREFIX = "com.android.appsearch.flags.";

    // The full string values for flags defined in the framework.
    //
    // The values of the static variables are the names of the flag defined in the framework's
    // aconfig files. E.g. "enable_safe_parcelable", with FLAG_PREFIX as the prefix.
    //
    // The name of the each static variable should be "FLAG_" + capitalized value of the flag.

    /** Enable SafeParcelable related features. */
    public static final String FLAG_ENABLE_SAFE_PARCELABLE = FLAG_PREFIX + "enable_safe_parcelable";

    /** Enable the "hasProperty" function in list filter query expressions. */
    public static final String FLAG_ENABLE_LIST_FILTER_HAS_PROPERTY_FUNCTION =
            FLAG_PREFIX + "enable_list_filter_has_property_function";

    /** Enable Schema Type Grouping related features. */
    public static final String FLAG_ENABLE_GROUPING_TYPE_PER_SCHEMA =
            FLAG_PREFIX + "enable_grouping_type_per_schema";

    /** Enable GenericDocument to take another GenericDocument to copy construct. */
    public static final String FLAG_ENABLE_GENERIC_DOCUMENT_COPY_CONSTRUCTOR =
            FLAG_PREFIX + "enable_generic_document_copy_constructor";

    // Whether the features should be enabled.
    //
    // In Jetpack, those should always return true.

    /** Whether SafeParcelable should be enabled. */
    public static boolean enableSafeParcelable() {
        return true;
    }

    /** Whether the "hasProperty" function in list filter query expressions should be enabled. */
    public static boolean enableListFilterHasPropertyFunction() {
        return true;
    }

    /** Whether Schema Type Grouping should be enabled. */
    public static boolean enableGroupingTypePerSchema() {
        return true;
    }

    /** Whether Generic Document Copy Constructing should be enabled. */
    public static boolean enableGenericDocumentCopyConstructor() {
        return true;
    }
}
