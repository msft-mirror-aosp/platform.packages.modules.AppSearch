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

import android.annotation.NonNull;
import android.app.appsearch.annotation.CanIgnoreReturnValue;

import java.util.Objects;

// TODO(b/319285816): link converter here.
/**
 * Class holds detailed stats of a click action, converted from {@link
 * android.app.appsearch.PutDocumentsRequest#getTakenActionGenericDocuments}.
 *
 * @hide
 */
public class ClickStats {
    private final long mTimestampMillis;

    private final long mTimeStayOnResultMillis;

    private final int mResultRankInBlock;

    private final int mResultRankGlobal;

    ClickStats(@NonNull Builder builder) {
        Objects.requireNonNull(builder);
        mTimestampMillis = builder.mTimestampMillis;
        mTimeStayOnResultMillis = builder.mTimeStayOnResultMillis;
        mResultRankInBlock = builder.mResultRankInBlock;
        mResultRankGlobal = builder.mResultRankGlobal;
    }

    /** Returns the click action timestamp in milliseconds since Unix epoch. */
    public long getTimestampMillis() {
        return mTimestampMillis;
    }

    /** Returns the time (duration) of the user staying on the clicked result. */
    public long getTimeStayOnResultMillis() {
        return mTimeStayOnResultMillis;
    }

    /** Returns the in-block rank of the clicked result. */
    public int getResultRankInBlock() {
        return mResultRankInBlock;
    }

    /** Returns the global rank of the clicked result. */
    public int getResultRankGlobal() {
        return mResultRankGlobal;
    }

    /** Builder for {@link ClickStats} */
    public static final class Builder {
        private long mTimestampMillis;

        private long mTimeStayOnResultMillis;

        private int mResultRankInBlock;

        private int mResultRankGlobal;

        /** Sets the click action timestamp in milliseconds since Unix epoch. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTimestampMillis(long timestampMillis) {
            mTimestampMillis = timestampMillis;
            return this;
        }

        /** Sets the time (duration) of the user staying on the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setTimeStayOnResultMillis(long timeStayOnResultMillis) {
            mTimeStayOnResultMillis = timeStayOnResultMillis;
            return this;
        }

        /** Sets the in-block rank of the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setResultRankInBlock(int resultRankInBlock) {
            mResultRankInBlock = resultRankInBlock;
            return this;
        }

        /** Sets the global rank of the clicked result. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder setResultRankGlobal(int resultRankGlobal) {
            mResultRankGlobal = resultRankGlobal;
            return this;
        }

        /** Builds a new {@link ClickStats} from the {@link ClickStats.Builder}. */
        @NonNull
        public ClickStats build() {
            return new ClickStats(/* builder= */ this);
        }
    }
}