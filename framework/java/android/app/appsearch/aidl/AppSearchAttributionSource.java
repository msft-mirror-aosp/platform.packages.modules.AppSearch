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

package android.app.appsearch.aidl;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresApi;
import android.app.Activity;
import android.content.AttributionSource;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;

import com.android.internal.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * Compatibility version of AttributionSource.
 *
 * Refactor AttributionSource to work on older API levels. For Android S+, this class maintains the
 * original implementation of AttributionSource methods. However, for Android R-, this class
 * creates a new implementation.
 * Replace calls to AttributionSource with AppSearchAttributionSource.
 * For a given Context, replace calls to getAttributionSource with createAttributionSource.
 *
 * @hide
 */
// TODO(b/275629842): Update the class to use SafeParcelable instead of Parcelable.
public final class AppSearchAttributionSource implements Parcelable {

    private final Compat mCompat;
    private final String mCallingPackageName;
    private final int mCallingUid;

    /**
     * Constructs an instance of AppSearchAttributionSource.
     * @param compat The compat version that provides AttributionSource implementation on
     *      lower API levels.
     */
    private AppSearchAttributionSource(@NonNull Compat compat) {
        mCompat = Objects.requireNonNull(compat);
        mCallingPackageName = mCompat.getPackageName();
        mCallingUid = mCompat.getUid();
    }

    @VisibleForTesting
    public AppSearchAttributionSource(@NonNull String callingPackageName, int callingUid) {
        mCallingPackageName = Objects.requireNonNull(callingPackageName);
        mCallingUid = callingUid;

        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AttributionSource attributionSource =
                new AttributionSource.Builder(mCallingUid)
                        .setPackageName(mCallingPackageName).build();
            mCompat = new Api31Impl(attributionSource);
        } else {
            mCompat = new Api19Impl(mCallingPackageName, mCallingUid);
        }
    }

    private AppSearchAttributionSource(@NonNull Parcel in) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Deserializing attributionSource calls enforceCallingUidAndPid similar to
            // AttributionSource.
            AttributionSource attributionSource =
                in.readParcelable(AttributionSource.class.getClassLoader());
            mCompat = new Api31Impl(attributionSource);
            mCallingPackageName = mCompat.getPackageName();
            mCallingUid = mCompat.getUid();
        } else {
            mCallingPackageName = Objects.requireNonNull(in.readString());
            mCallingUid = in.readInt();
            Api19Impl impl = new Api19Impl(mCallingPackageName, mCallingUid);
            // Enforce calling pid and uid must be called here on R and below similar to how
            // AttributionSource is implemented.
            impl.enforceCallingUid();
            impl.enforceCallingPid();
            mCompat = impl;
        }
    }

    /**
     * Provides a backward-compatible wrapper for AttributionSource.
     *
     * This method is not supported on devices running SDK <= 30(R) since the AttributionSource
     * class will not be available.
     *
     * @param attributionSource AttributionSource class to wrap, must not be null
     * @return wrapped class
     */
    @RequiresApi(Build.VERSION_CODES.S)
    @NonNull
    private static AppSearchAttributionSource toAppSearchAttributionSource(
        @NonNull AttributionSource attributionSource) {
        return new AppSearchAttributionSource(new Api31Impl(attributionSource));
    }

    /**
     * Provides a backward-compatible wrapper for AttributionSource.
     *
     * This method is not supported on devices running SDK <= 19(H) since the AttributionSource
     * class will not be available.
     *
     * @param packageName The package name to wrap, must not be null
     * @param uid The uid to wrap
     * @return wrapped class
     */
    private static AppSearchAttributionSource toAppSearchAttributionSource(
        @NonNull String packageName, int uid) {
        return new AppSearchAttributionSource(
            new Api19Impl(packageName, uid));
    }

    /**
     * Create an instance of AppSearchAttributionSource.
     *
     * @param context Context the application is running on.
     */
    public static AppSearchAttributionSource createAttributionSource(
        @NonNull Context context) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return toAppSearchAttributionSource(context.getAttributionSource());
        }

        return toAppSearchAttributionSource(context.getPackageName(), Process.myUid());
    }

    /**
     * Return AttributionSource on Android S+ and return null on Android R-.
     */
    @Nullable
    public AttributionSource getAttributionSource() {
        return mCompat.getAttributionSource();
    }

    @Nullable
    public String getPackageName() {
        return mCompat.getPackageName();
    }

    public int getUid() {
        return mCompat.getUid();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        if (VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dest.writeParcelable(mCompat.getAttributionSource(), flags);
        } else {
            dest.writeString(mCompat.getPackageName());
            dest.writeInt(mCompat.getUid());
        }
    }

    public static final Creator<AppSearchAttributionSource> CREATOR =
        new Creator<>() {
            @Override
            public AppSearchAttributionSource createFromParcel(Parcel in) {
                return new AppSearchAttributionSource(in);
            }

            @Override
            public AppSearchAttributionSource[] newArray(int size) {
                return new AppSearchAttributionSource[size];
            }
        };

    /** Compat class for AttributionSource to provide implementation for lower API levels. */
    private interface Compat {
        /** The package that is accessing the permission protected data. */
        @Nullable
        String getPackageName();

        /** The attribution source of the app accessing the permission protected data. */
        @Nullable
        AttributionSource getAttributionSource();

        /** The UID that is accessing the permission protected data. */
        int getUid();
    }

    @RequiresApi(VERSION_CODES.S)
    private static final class Api31Impl implements Compat {

        private final AttributionSource mAttributionSource;

        /**
         * Creates a new implementation for AppSearchAttributionSource's Compat for API levels 31+.
         *
         * @param attributionSource The attribution source that is accessing permission
         *      protected data.
         */
        Api31Impl(@NonNull AttributionSource attributionSource) {
            mAttributionSource = attributionSource;
        }

        @Override
        @Nullable
        public String getPackageName() {
            return mAttributionSource.getPackageName();
        }

        @Nullable
        @Override
        public AttributionSource getAttributionSource() {
            return mAttributionSource;
        }

        @Override
        public int getUid() {
            return mAttributionSource.getUid();
        }
    }

    private static class Api19Impl implements Compat {

        private final String mPackageName;
        private final int mUid;

        /**
         * Creates a new implementation for AppSearchAttributionSource's Compat for API levels 19+.
         *
         * @param packageName The package name that is accessing permission protected data.
         * @param uid The uid that is accessing permission protected data.
         */
        Api19Impl(String packageName, int uid) {
            mPackageName = packageName;
            mUid = uid;
        }

        @Override
        @Nullable
        public String getPackageName() {
            return mPackageName;
        }

        @Nullable
        @Override
        public AttributionSource getAttributionSource() {
            // AttributionSource class was added in Api level 31 and hence it is unavailable on API
            // levels lower than 31. This class is used in AppSearch to get package name, uid etc,
            // this implementation has util methods for getPackageName, getUid etc which could
            // be used instead.
            return null;
        }

        @Override
        public int getUid() {
            return mUid;
        }

        /**
         * If you are handling an IPC and you don't trust the caller you need to validate whether
         * the attribution source is one for the calling app to prevent the caller to pass you a
         * source from another app without including themselves in the attribution chain.
         *
         * @throws SecurityException if the attribution source cannot be trusted to be from
         *         the caller.
         */
        private void enforceCallingUid() {
            if (!checkCallingUid()) {
                int callingUid = Binder.getCallingUid();
                throw new SecurityException(
                    "Calling uid: " + callingUid + " doesn't match source uid: " + mUid);
            }
            // The verification for calling package happens in the service during API call.
        }

        /**
         * If you are handling an IPC and you don't trust the caller you need to validate whether
         * the attribution source is one for the calling app to prevent the caller to pass you a
         * source from another app without including themselves in the attribution chain.
         *
         * @return if the attribution source cannot be trusted to be from the caller.
         */
        private boolean checkCallingUid() {
            final int callingUid = Binder.getCallingUid();
            if (callingUid != mUid) {
                return false;
            }
            // The verification for calling package happens in the service during API call.
            return true;
        }

        /**
         * Validate that the call is happening on a Binder transaction.
         *
         * @throws SecurityException if the attribution source cannot be trusted to be from
         *         the caller.
         */
        private void enforceCallingPid() {
            if (!checkCallingPid()) {
                throw new SecurityException(
                    "Calling pid: "
                        + Binder.getCallingPid()
                        + " is same as process pid: "
                        + Process.myPid());
            }
        }

        /**
         * Validate that the call is happening on a Binder transaction.
         *
         * @return if the call is happening on the Binder thread.
         */
        private boolean checkCallingPid() {
            final int callingPid = Binder.getCallingPid();
            if (callingPid == Process.myPid()) {
                // Only call this on the binder thread. If a new thread is created to handle the
                // client request, Binder.getCallingPid() will return the thread's own pid.
                return false;
            }
            return true;
        }
    }
}