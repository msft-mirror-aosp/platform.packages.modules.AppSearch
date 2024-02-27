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
import android.app.appsearch.observer.ObserverCallback;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.UserHandle;

import java.util.Objects;

/**
 * Encapsulates a request to make a binder call to remove a previously registered observer.
 * @hide
 */
@SafeParcelable.Class(creator = "UnregisterObserverCallbackAidlRequestCreator")
public class UnregisterObserverCallbackAidlRequest extends AbstractSafeParcelable {
    @NonNull
    public static final UnregisterObserverCallbackAidlRequestCreator CREATOR =
            new UnregisterObserverCallbackAidlRequestCreator();

    @NonNull
    @Field(id = 1, getter = "getCallerAttributionSource")
    private final AppSearchAttributionSource mCallerAttributionSource;
    @NonNull
    @Field(id = 2, getter = "getObservedPackage")
    private final String mObservedPackage;
    @NonNull
    @Field(id = 3, getter = "getUserHandle")
    private final UserHandle mUserHandle;
    @Field(id = 4, getter = "getBinderCallStartTimeMillis")
    private final @ElapsedRealtimeLong long mBinderCallStartTimeMillis;

    /**
     * Removes previously registered {@link ObserverCallback} instances from the system.
     *
     * @param callerAttributionSource The permission identity of the package that owns the observer
     * @param observedPackage Package whose changes are being monitored
     * @param userHandle Handle of the calling user
     * @param binderCallStartTimeMillis start timestamp of binder call in Millis
     */
    @Constructor
    public UnregisterObserverCallbackAidlRequest(
            @Param(id = 1) @NonNull AppSearchAttributionSource callerAttributionSource,
            @Param(id = 2) @NonNull String observedPackage,
            @Param(id = 3) @NonNull UserHandle userHandle,
            @Param(id = 4) @ElapsedRealtimeLong long binderCallStartTimeMillis) {
        mCallerAttributionSource = Objects.requireNonNull(callerAttributionSource);
        mObservedPackage = Objects.requireNonNull(observedPackage);
        mUserHandle = Objects.requireNonNull(userHandle);
        mBinderCallStartTimeMillis = binderCallStartTimeMillis;
    }

    @NonNull
    public AppSearchAttributionSource getCallerAttributionSource() {
        return mCallerAttributionSource;
    }

    @NonNull
    public String getObservedPackage() {
        return mObservedPackage;
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
        UnregisterObserverCallbackAidlRequestCreator.writeToParcel(this, dest, flags);
    }
}