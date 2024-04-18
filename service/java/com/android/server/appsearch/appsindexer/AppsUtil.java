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

package com.android.server.appsearch.appsindexer;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.util.LogUtil;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.android.server.appsearch.appsindexer.appsearchtypes.MobileApplication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Utility class for pulling apps details from package manager. */
public final class AppsUtil {
    public static final String TAG = "AppSearchAppsUtil";

    private AppsUtil() {}

    /** Gets the resource Uri given a resource id. */
    @NonNull
    private static Uri getResourceUri(@NonNull PackageManager packageManager,
            @NonNull ApplicationInfo appInfo, int resourceId)
            throws PackageManager.NameNotFoundException {
        Objects.requireNonNull(packageManager);
        Objects.requireNonNull(appInfo);
        Resources resources = packageManager.getResourcesForApplication(appInfo);
        String resPkg = resources.getResourcePackageName(resourceId);
        String type = resources.getResourceTypeName(resourceId);
        return makeResourceUri(appInfo.packageName, resPkg, type, resourceId);
    }

    /**
     * Appends the resource id instead of name to make the resource uri due to b/161564466. The
     * resource names for some apps (e.g. Chrome) are obfuscated due to resource name collapsing,
     * so we need to use resource id instead.
     *
     * @see Uri
     */
    @NonNull
    private static Uri makeResourceUri(@NonNull String appPkg, @NonNull String resPkg,
            @NonNull String type, int resourceId) {
        Objects.requireNonNull(appPkg);
        Objects.requireNonNull(resPkg);
        Objects.requireNonNull(type);

        // For more details on Android URIs, see the official Android documentation:
        // https://developer.android.com/guide/topics/providers/content-provider-basics#ContentURIs
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(ContentResolver.SCHEME_ANDROID_RESOURCE);
        uriBuilder.encodedAuthority(appPkg);
        uriBuilder.appendEncodedPath(type);
        if (!appPkg.equals(resPkg)) {
            uriBuilder.appendEncodedPath(resPkg + ":" + resourceId);
        } else {
            uriBuilder.appendEncodedPath(String.valueOf(resourceId));
        }
        return uriBuilder.build();
    }

    /**
     * Gets the icon uri for the activity.
     *
     * @return the icon Uri string, or null if there is no icon resource.
     */
    @Nullable
    private static String getActivityIconUriString(
            @NonNull PackageManager packageManager, @NonNull ActivityInfo activityInfo) {
        Objects.requireNonNull(packageManager);
        Objects.requireNonNull(activityInfo);
        int iconResourceId = activityInfo.getIconResource();
        if (iconResourceId == 0) {
            return null;
        }

        try {
            return getResourceUri(
                    packageManager, activityInfo.applicationInfo, iconResourceId).toString();
        } catch (PackageManager.NameNotFoundException e) {
            // If resources aren't found for the application, that is fine. We return null and
            // handle it with getActivityIconUriString
            return null;
        }
    }

    /**
     * Uses {@link PackageManager} and a list of {@link PackageInfo} to convert the package infos
     * into AppSearch MobileApplication documents.
     *
     * @param packageInfos A list of PackageInfos to convert to MobileApplications
     * @see PackageManager#queryIntentActivities
     */
    @NonNull
    public static List<MobileApplication> buildAppsFromPackageInfos(
            @NonNull PackageManager packageManager, @NonNull List<PackageInfo> packageInfos) {
        Objects.requireNonNull(packageManager);
        Objects.requireNonNull(packageInfos);
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(null);
        List<ResolveInfo> activities;
        activities = packageManager.queryIntentActivities(intent, 0);
        Map<String, ResolveInfo> packageNameToLauncher = new ArrayMap<>();
        for (int i = 0; i < activities.size(); i++) {
            ResolveInfo ri = activities.get(i);
            packageNameToLauncher.put(ri.activityInfo.packageName, ri);
        }

        List<MobileApplication> mobileApplications = new ArrayList<>();
        for (int i = 0; i < packageInfos.size(); i++) {
            PackageInfo packageInfo = packageInfos.get(i);
            ResolveInfo resolveInfo = packageNameToLauncher.get(packageInfo.packageName);
            if (resolveInfo == null) {
                continue;
            }

            MobileApplication mobileApplication =
                    createMobileApplication(packageManager, packageInfo, resolveInfo);
            if (mobileApplication != null && !mobileApplication.getDisplayName().isEmpty()) {
                mobileApplications.add(mobileApplication);
            }

        }
        return mobileApplications;
    }

    /** Gets the SHA-256 certificate from a {@link PackageManager}, or null if it is not found */
    @Nullable
    private static byte[] getCertificate(@NonNull PackageInfo packageInfo) {
        Objects.requireNonNull(packageInfo);
        if (packageInfo.signingInfo == null) {
            if (LogUtil.DEBUG) {
                Log.d(TAG, "Signing info not found for package: " + packageInfo.packageName);
            }
            return null;
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA256");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        md.update(packageInfo.signingInfo.getSigningCertificateHistory()[0].toByteArray());
        return md.digest();
    }

    /**
     * Uses PackageManager to supplement packageInfos with an application display name and icon uri.
     *
     * @return a MobileApplication representing the packageInfo, null if finding the signing
     * certificate fails.
     */
    @Nullable
    private static MobileApplication createMobileApplication(
            @NonNull PackageManager packageManager,
            @NonNull PackageInfo packageInfo,
            @NonNull ResolveInfo resolveInfo) {
        Objects.requireNonNull(packageManager);
        Objects.requireNonNull(packageInfo);
        Objects.requireNonNull(resolveInfo);

        String applicationDisplayName = resolveInfo.loadLabel(packageManager).toString();
        if (TextUtils.isEmpty(applicationDisplayName)) {
            applicationDisplayName = packageInfo.applicationInfo.className;
        }

        String iconUri = getActivityIconUriString(packageManager, resolveInfo.activityInfo);

        byte[] certificate = getCertificate(packageInfo);
        if (certificate == null) {
            return null;
        }

        MobileApplication.Builder builder =
                new MobileApplication.Builder(packageInfo.packageName, certificate)
                        .setDisplayName(applicationDisplayName)
                        // TODO(b/275592563): Populate with nicknames from various sources
                        .setCreationTimestampMillis(packageInfo.firstInstallTime)
                        .setUpdatedTimestampMs(packageInfo.lastUpdateTime);

        if (iconUri != null) {
            builder.setIconUri(iconUri);
        }

        if (resolveInfo.activityInfo.name != null) {
            builder.setClassName(resolveInfo.activityInfo.name);
        }
        return builder.build();
    }
}