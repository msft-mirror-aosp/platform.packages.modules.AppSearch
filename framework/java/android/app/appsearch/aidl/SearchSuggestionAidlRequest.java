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

package android.app.appsearch.aidl;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.app.appsearch.SearchSuggestionSpec;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to retrieve suggested search strings.
 *
 * @hide
 */
@SafeParcelable.Class(creator = "SearchSuggestionAidlRequestCreator")
public class SearchSuggestionAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<SearchSuggestionAidlRequest> CREATOR =
            new SearchSuggestionAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getDatabaseName")
    private final String mDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getSuggestionQueryExpression")
    private final String mSuggestionQueryExpression;

    @NonNull
    @Field(id = 4, getter = "getSearchSuggestionSpec")
    private final SearchSuggestionSpec mSearchSuggestionSpec;

    @NonNull
    @Field(id = 5, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 6, getter = "getBinderCallStartTimeMillis")
    private final @ElapsedRealtimeLong long mBinderCallStartTimeMillis;

    /**
     * Retrieves suggested Strings that could be used as {@code queryExpression} in search API.
     *
     * @param callerAttributionSource The permission identity of the package to suggest over.
     * @param databaseName The databaseName this suggest is for.
     * @param suggestionQueryExpression the non empty query string to search suggestions
     * @param searchSuggestionSpec SearchSuggestionSpec describing what to suggest
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public SearchSuggestionAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String databaseName,
            @Param(id = 3) @NonNull String suggestionQueryExpression,
            @Param(id = 4) @NonNull SearchSuggestionSpec searchSuggestionSpec,
            @Param(id = 5) @NonNull UserHandle userHandle,
            @Param(id = 6) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mDatabaseName = Objects.requireNonNull(databaseName);
        mSuggestionQueryExpression = Objects.requireNonNull(suggestionQueryExpression);
        mSearchSuggestionSpec = Objects.requireNonNull(searchSuggestionSpec);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @NonNull
    public String getDatabaseName() {
        return mDatabaseName;
    }

    @NonNull
    public String getSuggestionQueryExpression() {
        return mSuggestionQueryExpression;
    }

    @NonNull
    public SearchSuggestionSpec getSearchSuggestionSpec() {
        return mSearchSuggestionSpec;
    }

    @NonNull
    public UserHandle getUserHandle() {
        return mUserHandle;
    }

    @ElapsedRealtimeLong
    public long getBinderCallStartTimeMillis() {
        return mBinderCallStartTimeMillis;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        SearchSuggestionAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
