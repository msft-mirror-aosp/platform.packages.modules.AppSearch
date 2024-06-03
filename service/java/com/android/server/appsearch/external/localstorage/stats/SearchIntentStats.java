/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.server.appsearch.external.localstorage.stats;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.annotation.CanIgnoreReturnValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

// TODO(b/319285816): link converter here.
/**
 * Class holds detailed stats of a search intent, converted from {@link
 * android.app.appsearch.PutDocumentsRequest#getTakenActionGenericDocuments}.
 *
 * <p>A search intent includes a valid AppSearch search request, potentially followed by several
 * user click actions (see {@link ClickStats}) on fetched result documents. Related information of a
 * search intent will be extracted from {@link
 * android.app.appsearch.PutDocumentsRequest#getTakenActionGenericDocuments}.
 *
 * @hide
 */
public final class SearchIntentStats {
    /** AppSearch query correction type compared with the previous query. */
    @IntDef(
            value = {
                QUERY_CORRECTION_TYPE_UNKNOWN,
                QUERY_CORRECTION_TYPE_FIRST_QUERY,
                QUERY_CORRECTION_TYPE_REFINEMENT,
                QUERY_CORRECTION_TYPE_ABANDONMENT,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface QueryCorrectionType {}

    public static final int QUERY_CORRECTION_TYPE_UNKNOWN = 0;

    public static final int QUERY_CORRECTION_TYPE_FIRST_QUERY = 1;

    public static final int QUERY_CORRECTION_TYPE_REFINEMENT = 2;

    public static final int QUERY_CORRECTION_TYPE_ABANDONMENT = 3;

    @NonNull private final String mPackageName;

    @Nullable private final String mDatabase;

    @Nullable private final String mPrevQuery;

    @Nullable private final String mCurrQuery;

    private final long mTimestampMillis;

    private final int mNumResultsFetched;

    @QueryCorrectionType private final int mQueryCorrectionType;

    @NonNull private final List<ClickStats> mClicksStats;

    SearchIntentStats(@NonNull Builder builder) {
        Objects.requireNonNull(builder);
        mPackageName = builder.mPackageName;
        mDatabase = builder.mDatabase;
        mPrevQuery = builder.mPrevQuery;
        mCurrQuery = builder.mCurrQuery;
        mTimestampMillis = builder.mTimestampMillis;
        mNumResultsFetched = builder.mNumResultsFetched;
        mQueryCorrectionType = builder.mQueryCorrectionType;
        mClicksStats = builder.mClicksStats;
    }

    /** Returns calling package name. */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns calling database name. */
    @Nullable
    public String getDatabase() {
        return mDatabase;
    }

    /** Returns the raw query string of the previous search intent. */
    @Nullable
    public String getPrevQuery() {
        return mPrevQuery;
    }

    /** Returns the raw query string of this (current) search intent. */
    @Nullable
    public String getCurrQuery() {
        return mCurrQuery;
    }

    /** Returns the search intent timestamp in milliseconds since Unix epoch. */
    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    /**
     * Returns total number of results fetched from AppSearch by the client in this search intent.
     */
    public int getNumResultsFetched() {
        return mNumResultsFetched;
    }

    /**
     * Returns the correction type of the query in this search intent compared with the previous
     * search intent. Default value: {@link SearchIntentStats#QUERY_CORRECTION_TYPE_UNKNOWN}.
     */
    @QueryCorrectionType
    public int getQueryCorrectionType() {
        return mQueryCorrectionType;
    }

    /** Returns the list of {@link ClickStats} in this search intent. */
    @NonNull
    public List<ClickStats> getClicksStats() {
        return mClicksStats;
    }

    /** Builder for {@link SearchIntentStats} */
    public static final class Builder {
        @NonNull private final String mPackageName;

        @Nullable private String mDatabase;

        @Nullable private String mPrevQuery;

        @Nullable private String mCurrQuery;

        private long mTimestampMillis;

        private int mNumResultsFetched;

        @QueryCorrectionType private int mQueryCorrectionType = QUERY_CORRECTION_TYPE_UNKNOWN;

        @NonNull private List<ClickStats> mClicksStats = new ArrayList<>();

        private boolean mBuilt = false;

        /** Constructor for the {@link Builder}. */
        public Builder(@NonNull String packageName) {
            mPackageName = Objects.requireNonNull(packageName);
        }

        /** Sets calling database name. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setDatabase(@Nullable String database) {
            resetIfBuilt();
            mDatabase = database;
            return this;
        }

        /** Sets the raw query string of the previous search intent. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setPrevQuery(@Nullable String prevQuery) {
            resetIfBuilt();
            mPrevQuery = prevQuery;
            return this;
        }

        /** Sets the raw query string of this (current) search intent. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setCurrQuery(@Nullable String currQuery) {
            resetIfBuilt();
            mCurrQuery = currQuery;
            return this;
        }

        /** Sets the search intent timestamp in milliseconds since Unix epoch. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTimestampMillis(long timestampMillis) {
            resetIfBuilt();
            mTimestampMillis = timestampMillis;
            return this;
        }

        /**
         * Sets total number of results fetched from AppSearch by the client in this search intent.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setNumResultsFetched(int numResultsFetched) {
            resetIfBuilt();
            mNumResultsFetched = numResultsFetched;
            return this;
        }

        /**
         * Sets the correction type of the query in this search intent compared with the previous
         * search intent.
         */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setQueryCorrectionType(@QueryCorrectionType int queryCorrectionType) {
            resetIfBuilt();
            mQueryCorrectionType = queryCorrectionType;
            return this;
        }

        /** Adds one or more {@link ClickStats} objects to this search intent. */
        @CanIgnoreReturnValue
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addClicksStats(@NonNull ClickStats... clicksStats) {
            Objects.requireNonNull(clicksStats);
            resetIfBuilt();
            return addClicksStats(Arrays.asList(clicksStats));
        }

        /** Adds a collection of {@link ClickStats} objects to this search intent. */
        @CanIgnoreReturnValue
        @SuppressWarnings("MissingGetterMatchingBuilder")
        @NonNull
        public Builder addClicksStats(@NonNull Collection<? extends ClickStats> clicksStats) {
            Objects.requireNonNull(clicksStats);
            resetIfBuilt();
            mClicksStats.addAll(clicksStats);
            return this;
        }

        /**
         * If built, make a copy of previous data for every field so that the builder can be reused.
         */
        private void resetIfBuilt() {
            if (mBuilt) {
                mClicksStats = new ArrayList<>(mClicksStats);
                mBuilt = false;
            }
        }

        /** Builds a new {@link SearchIntentStats} from the {@link Builder}. */
        @NonNull
        public SearchIntentStats build() {
            mBuilt = true;
            return new SearchIntentStats(/* builder= */ this);
        }
    }
}
