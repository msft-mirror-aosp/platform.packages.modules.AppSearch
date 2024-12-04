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
import android.app.appsearch.AppSearchBlobHandle;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to remove a batch of blob to AppSearch
 *
 * @hide
 */
@SafeParcelable.Class(creator = "RemoveBlobAidlRequestCreator")
public final class RemoveBlobAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final Parcelable.Creator<RemoveBlobAidlRequest> CREATOR =
            new RemoveBlobAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;

    @NonNull
    @Field(id = 2, getter = "getCallingDatabaseName")
    private final String mCallingDatabaseName;

    @NonNull
    @Field(id = 3, getter = "getBlobHandles")
    private final List<AppSearchBlobHandle> mBlobHandles;

    @NonNull
    @Field(id = 4, getter = "getUserHandle")
    private final UserHandle mUserHandle;

    @Field(id = 5, getter = "getBinderCallStartTimeMillis")
    private final long mBinderCallStartTimeMillis;

    /**
     * Commit a batch of blob to AppSearch
     *
     * @param callerAttributionSource The permission identity of the package that is getting this
     *     document.
     * @param callingDatabaseName The database name of these blob stored in.
     * @param blobHandles The blobs to remove
     * @param userHandle Handle of the calling user.
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis.
     */
    @Constructor
    public RemoveBlobAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String callingDatabaseName,
            @Param(id = 3) @NonNull List<AppSearchBlobHandle> blobHandles,
            @Param(id = 4) @NonNull UserHandle userHandle,
            @Param(id = 5) long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mCallingDatabaseName = Objects.requireNonNull(callingDatabaseName);
        mBlobHandles = Objects.requireNonNull(blobHandles);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @NonNull
    public String getCallingDatabaseName() {
        return mCallingDatabaseName;
    }

    /** Gets the {@code list} of {@link AppSearchBlobHandle} to remove blobs from AppSearch. */
    @NonNull
    public List<AppSearchBlobHandle> getBlobHandles() {
        return Collections.unmodifiableList(mBlobHandles);
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
        RemoveBlobAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}
