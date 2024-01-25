/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.server.appsearch.visibilitystore;

import static android.Manifest.permission.READ_ASSISTANT_APP_SEARCH_DATA;
import static android.Manifest.permission.READ_CALENDAR;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_GLOBAL_APP_SEARCH_DATA;
import static android.Manifest.permission.READ_HOME_APP_SEARCH_DATA;
import static android.Manifest.permission.READ_SMS;
import static android.permission.PermissionManager.PERMISSION_GRANTED;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;
import android.app.appsearch.InternalVisibilityConfig;
import android.app.appsearch.PackageIdentifier;
import android.app.appsearch.SetSchemaRequest;
import android.app.appsearch.VisibilityConfig;
import android.app.appsearch.aidl.AppSearchAttributionSource;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserHandle;
import android.permission.PermissionManager;

import com.android.server.appsearch.external.localstorage.visibilitystore.CallerAccess;
import com.android.server.appsearch.external.localstorage.visibilitystore.VisibilityChecker;
import com.android.server.appsearch.external.localstorage.visibilitystore.VisibilityStore;
import com.android.server.appsearch.util.PackageManagerUtil;
import com.android.server.appsearch.util.PackageUtil;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A platform implementation of {@link VisibilityChecker}.
 *
 * @hide
 */
public class VisibilityCheckerImpl implements VisibilityChecker {
    // Context of the user that the call is being made as.
    private final Context mUserContext;
    private final PermissionManager mPermissionManager;
    private final DevicePolicyManager mDpm;

    public VisibilityCheckerImpl(@NonNull Context userContext) {
        mUserContext = Objects.requireNonNull(userContext);
        mPermissionManager = userContext.getSystemService(PermissionManager.class);
        mDpm = userContext.getSystemService(DevicePolicyManager.class);
    }

    @Override
    public boolean isSchemaSearchableByCaller(
            @NonNull CallerAccess callerAccess,
            @NonNull String packageName,
            @NonNull String prefixedSchema,
            @NonNull VisibilityStore visibilityStore) {
        Objects.requireNonNull(callerAccess);
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(prefixedSchema);
        if (packageName.equals(VisibilityStore.VISIBILITY_PACKAGE_NAME)) {
            return false; // VisibilityStore schemas are for internal bookkeeping.
        }

        FrameworkCallerAccess frameworkCallerAccess = (FrameworkCallerAccess) callerAccess;
        InternalVisibilityConfig internalVisibilityConfig =
                visibilityStore.getVisibility(prefixedSchema);

        // If caller requires enterprise access, the given schema is only visible if caller has all
        // required permissions.
        if (frameworkCallerAccess.isForEnterprise()) {
            return internalVisibilityConfig != null && isSchemaVisibleToPermission(
                    internalVisibilityConfig.getVisibilityConfig(),
                    frameworkCallerAccess.getCallingAttributionSource(),
                    /*checkEnterpriseAccess=*/ true);
        }

        if (internalVisibilityConfig == null) {
            // The target schema doesn't exist yet. We will treat it as default setting and the only
            // accessible case is when the caller has system access.
            return frameworkCallerAccess.doesCallerHaveSystemAccess();
        }

        // Check whether the calling package has system access and the target schema is visible to
        // the system.
        if (frameworkCallerAccess.doesCallerHaveSystemAccess() &&
                !internalVisibilityConfig.isNotDisplayedBySystem()) {
            return true;
        }

        // Check OR visibility settings. Caller could access if they match ANY of the requirements
        // in the visibilityConfig.
        VisibilityConfig visibilityConfig = internalVisibilityConfig.getVisibilityConfig();
        if (checkMatchAnyVisibilityConfig(
                frameworkCallerAccess, visibilityConfig)) {
            return true;
        }

        // Check AND visibility settings. Caller could access if they match ALL of the requirements
        // in the visibilityConfig.
        Set<VisibilityConfig> visibleToConfigs = internalVisibilityConfig.getVisibleToConfigs();
        for (VisibilityConfig visibleToConfig : visibleToConfigs) {
            if (checkMatchAllVisibilityConfig(
                    frameworkCallerAccess, visibleToConfig)) {
                return true;
            }
        }
        return false;
    }

    /**  Check whether the caller math ANY of the visibility requirements.     */
    private boolean checkMatchAnyVisibilityConfig(
        @NonNull FrameworkCallerAccess frameworkCallerAccess,
        @NonNull VisibilityConfig visibilityConfig) {
        if (isSchemaVisibleToPackages(visibilityConfig,
            frameworkCallerAccess.getCallingAttributionSource().getUid())) {
            // The caller is in the allow list and has access to the given schema.
            return true;
        }

        // Check whether caller has all required permissions for the given schema.
        if(isSchemaVisibleToPermission(visibilityConfig,
                frameworkCallerAccess.getCallingAttributionSource(),
                /*checkEnterpriseAccess=*/ false)) {
            return true;
        }

        // Check whether the calling package has visibility to the package providing the schema.
        return isSchemaPubliclyVisibleFromPackage(visibilityConfig, frameworkCallerAccess);
    }

    /**  Check whether the caller math ALL of the visibility requirements.     */
    private boolean checkMatchAllVisibilityConfig(
        @NonNull FrameworkCallerAccess frameworkCallerAccess,
        @NonNull VisibilityConfig visibilityConfig) {

        // We will skip following checks if user never specific them. But the caller should has
        // passed at least one check to get the access.
        boolean hasPassedCheck = false;

        // Check whether the caller is in the allow list and has access to the given schema.
        if (!visibilityConfig.getVisibleToPackages().isEmpty()) {
            if (!isSchemaVisibleToPackages(visibilityConfig,
                frameworkCallerAccess.getCallingAttributionSource().getUid())) {
                return false;// Return early for the 'ALL' case.
            }
            hasPassedCheck = true;
        }

        // Check whether caller has all required permissions for the given schema.
        // We could directly return the boolean results since it is the last checking.
        if (!visibilityConfig.getVisibleToPermissions().isEmpty()) {
            if (!isSchemaVisibleToPermission(visibilityConfig,
                frameworkCallerAccess.getCallingAttributionSource(),
                /*checkEnterpriseAccess=*/ false)) {
                return false;// Return early for the 'ALL' case.
            }
            hasPassedCheck = true;
        }

        // Check whether the calling package has visibility to the package providing the schema.
        if (visibilityConfig.getPubliclyVisibleTargetPackage() != null) {
            if (!isSchemaPubliclyVisibleFromPackage(visibilityConfig, frameworkCallerAccess)) {
                return false; // Return early for the 'ALL' case.
            }
            hasPassedCheck = true;
        }

        return hasPassedCheck;
    }

    private boolean isSchemaPubliclyVisibleFromPackage(@NonNull VisibilityConfig visibilityConfig,
            FrameworkCallerAccess frameworkCallerAccess) {
        PackageIdentifier targetPackage = visibilityConfig.getPubliclyVisibleTargetPackage();
        if (targetPackage == null) {
            return false;
        }

        // Ensure the sha 256 certificate matches the certificate of the actual publicly visible
        // target package.
        if (!PackageManagerUtil.hasSigningCertificate(mUserContext,
                targetPackage.getPackageName(), targetPackage.getSha256Certificate())) {
            return false;
        }

        // We cannot use the package name of the schema itself because the schema could be in a
        // separate package from the publicly visible target package. For instance, Apps Indexer
        // could store a document representing a timer app in a schema in the android package, but
        // the publicly visible target package for that schema could be the timer app package.
        try {
            // The call that opens up documents to "public" access
            if (mUserContext.getPackageManager().canPackageQuery(
                    frameworkCallerAccess.getCallingPackageName(),
                    targetPackage.getPackageName())) {
                return true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // One or both of the packages doesn't exist. Either way, we don't have public
            // visibility to the target package, so continue to return false.
        }
        return false;
    }

    /**
     * Returns whether the schema is accessible by the {@code callerUid}. Checks that the callerUid
     * has one of the allowed PackageIdentifier's package. And if so, that the package also has the
     * matching certificate.
     *
     * <p>This supports packages that have certificate rotation. As long as the specified
     * certificate was once used to sign the package, the package will still be granted access. This
     * does not handle packages that have been signed by multiple certificates.
     */
    private boolean isSchemaVisibleToPackages(@NonNull VisibilityConfig visibilityConfig,
            int callerUid) {
        List<PackageIdentifier> visibleToPackages = visibilityConfig.getVisibleToPackages();
        for (int i = 0; i < visibleToPackages.size(); i++) {
            PackageIdentifier visibleToPackage = visibleToPackages.get(i);

            // TODO(b/169883602): Consider caching the UIDs of packages. Looking this up in the
            // package manager could be costly. We would also need to update the cache on
            // package-removals.

            // 'callerUid' is the uid of the caller. The 'user' doesn't have to be the same one as
            // the callerUid since clients can createContextAsUser with some other user, and then
            // make calls to us. So just check if the appId portion of the uid is the same. This is
            // essentially UserHandle.isSameApp, but that's not a system API for us to use.
            int callerAppId = UserHandle.getAppId(callerUid);
            int packageUid = PackageUtil.getPackageUid(
                    mUserContext, visibleToPackage.getPackageName());
            int userAppId = UserHandle.getAppId(packageUid);
            if (callerAppId != userAppId) {
                continue;
            }

            // Check that the package also has the matching certificate
            if (PackageManagerUtil.hasSigningCertificate(
                    mUserContext,
                    visibleToPackage.getPackageName(),
                    visibleToPackage.getSha256Certificate())) {
                // The caller has the right package name and right certificate!
                return true;
            }
        }
        // If we can't verify the schema is package accessible, default to no access.
        return false;
    }

    /**
     * Returns whether the caller holds required permissions for the given schema.
     */
    private boolean isSchemaVisibleToPermission(@NonNull VisibilityConfig visibilityConfig,
            @Nullable AppSearchAttributionSource callerAttributionSource,
            boolean checkEnterpriseAccess) {
        Set<Set<Integer>> visibleToPermissions = visibilityConfig.getVisibleToPermissions();
        if (visibleToPermissions.isEmpty() || callerAttributionSource == null) {
            // Provider doesn't set any permissions or there is no caller attribution source,
            // default is not accessible to anyone.
            return false;
        }
        for (Set<Integer> allRequiredPermissions : visibleToPermissions) {
            // User may set multiple required permission sets. Provider need to hold ALL required
            // permission of ANY of the individual value sets.
            if (doesCallerHoldsAllRequiredPermissions(allRequiredPermissions,
                    callerAttributionSource, checkEnterpriseAccess)) {
                // The calling package has all required permissions in this set, return true.
                return true;
            }
        }
        // The calling doesn't hold all required permissions for any individual sets, return false.
        return false;
    }

    /** Returns true if the caller holds all required permission in the given set. */
    private boolean doesCallerHoldsAllRequiredPermissions(
            @NonNull Set<Integer> allRequiredPermissions,
            @NonNull AppSearchAttributionSource callerAttributionSource,
            boolean checkEnterpriseAccess) {
        // TODO(b/319310107): change the handling for enterprise permissions; currently, we ignore
        //  enterprise permissions for non-enterprise calls, but this doesn't match how required
        //  permissions are intended to work
        // If the caller requires enterprise access, the caller must pass both enterprise
        // permission and Android permission checks.
        if (checkEnterpriseAccess) {
            // If ENTERPRISE_ACCESS is not in the set of required permissions, the given schema is
            // not visible to enterprise sessions. ENTERPRISE_ACCESS permission does not limit a
            // schema to only enterprise access however.
            if (!allRequiredPermissions.contains(SetSchemaRequest.ENTERPRISE_ACCESS)) {
                return false;
            }
            // Enterprise access to contacts requires the caller to have either managed profile
            // caller id or contacts search access.
            // TODO(b/319308885): write a test to verify we're handling this permission correctly
            if (allRequiredPermissions.contains(SetSchemaRequest.ENTERPRISE_CONTACTS_DEVICE_POLICY)
                    && !allowedByEnterpriseContactsDevicePolicy(
                    callerAttributionSource.getPackageName())) {
                return false;
            }
        }

        for (int requiredPermission : allRequiredPermissions) {
            String permission;
            switch (requiredPermission) {
                case SetSchemaRequest.READ_SMS:
                    permission = READ_SMS;
                    break;
                case SetSchemaRequest.READ_CALENDAR:
                    permission = READ_CALENDAR;
                    break;
                case SetSchemaRequest.READ_CONTACTS:
                    permission = READ_CONTACTS;
                    break;
                case SetSchemaRequest.READ_EXTERNAL_STORAGE:
                    permission = READ_EXTERNAL_STORAGE;
                    break;
                case SetSchemaRequest.READ_HOME_APP_SEARCH_DATA:
                    permission = READ_HOME_APP_SEARCH_DATA;
                    break;
                case SetSchemaRequest.READ_ASSISTANT_APP_SEARCH_DATA:
                    permission = READ_ASSISTANT_APP_SEARCH_DATA;
                    break;
                case SetSchemaRequest.ENTERPRISE_ACCESS:
                case SetSchemaRequest.ENTERPRISE_CONTACTS_DEVICE_POLICY:
                    // These enterprise permissions don't correspond to Android permissions
                    continue;
                default:
                    throw new UnsupportedOperationException(
                            "The required permission is unsupported in AppSearch : "
                                    + requiredPermission);
            }
            // getAttributionSource can be safely called and the returned value will only be
            // null on Android R-
            if (PERMISSION_GRANTED != mPermissionManager.checkPermissionForDataDelivery(
                    permission, callerAttributionSource.getAttributionSource(),
                    /*message=*/"appsearch")) {
                // The calling package doesn't have this required permission, return false.
                return false;
            }
        }
        // The calling package has all required permissions in this set, return true.
        return true;
    }

    /**
     * Checks whether or not the calling package has caller id or contacts access to the work
     * profile. This check is only done for enterprise access and therefore always runs in the work
     * profile's AppSearch/VisibilityChecker instance.
     *
     * <p>Note, this method uses api's only supported on U+ and therefore returns false on devices
     * below U.
     */
    private boolean allowedByEnterpriseContactsDevicePolicy(String callingPackageName) {
        // This is the same check CP2 makes to determine whether or not a user has access to
        // enterprise contacts. Accessible fields are restricted to match the allowed fields for
        // caller id access which is more limited than contacts access.
        // https://cs.android.com/android/platform/superproject/main/+/main:packages/providers/ContactsProvider/src/com/android/providers/contacts/enterprise/EnterprisePolicyGuard.java;l=121;drc=242bb9f25b210fbfe36a384088221b54b2602b34
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // These api's are only supported on U+
            return mDpm.hasManagedProfileCallerIdAccess(mUserContext.getUser(), callingPackageName)
                    || mDpm.hasManagedProfileContactsAccess(mUserContext.getUser(),
                    callingPackageName);
        }
        return false;
    }

    /**
     * Checks whether the given package has access to system-surfaceable schemas.
     *
     * @param callerPackageName Package name of the caller.
     */
    public boolean doesCallerHaveSystemAccess(@NonNull String callerPackageName) {
        Objects.requireNonNull(callerPackageName);
        return mUserContext.getPackageManager()
                .checkPermission(READ_GLOBAL_APP_SEARCH_DATA, callerPackageName)
                == PackageManager.PERMISSION_GRANTED;
    }
}
