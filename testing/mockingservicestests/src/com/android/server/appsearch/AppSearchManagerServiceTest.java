/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.server.appsearch;

import static android.system.OsConstants.O_RDONLY;
import static android.system.OsConstants.O_WRONLY;

import static com.android.internal.util.ConcurrentUtils.DIRECT_EXECUTOR;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.UiAutomation;
import android.app.appsearch.AppSearchResult;
import android.app.appsearch.AppSearchSchema;
import android.app.appsearch.GenericDocument;
import android.app.appsearch.InternalSetSchemaResponse;
import android.app.appsearch.aidl.AppSearchBatchResultParcel;
import android.app.appsearch.aidl.AppSearchResultParcel;
import android.app.appsearch.aidl.DocumentsParcel;
import android.app.appsearch.aidl.IAppSearchBatchResultCallback;
import android.app.appsearch.aidl.IAppSearchManager;
import android.app.appsearch.aidl.IAppSearchObserverProxy;
import android.app.appsearch.aidl.IAppSearchResultCallback;
import android.app.appsearch.stats.SchemaMigrationStats;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.dx.mockito.inline.extended.StaticMockitoSessionBuilder;
import com.android.modules.utils.testing.StaticMockFixture;
import com.android.modules.utils.testing.StaticMockFixtureRule;
import com.android.modules.utils.testing.TestableDeviceConfig;
import com.android.server.LocalManagerRegistry;
import com.android.server.appsearch.external.localstorage.stats.CallStats;
import com.android.server.appsearch.external.localstorage.stats.SearchStats;
import com.android.server.appsearch.external.localstorage.stats.SetSchemaStats;
import com.android.server.appsearch.stats.PlatformLogger;
import com.android.server.usage.StorageStatsManagerLocal;

import libcore.io.IoBridge;

import com.google.common.util.concurrent.SettableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AppSearchManagerServiceTest {
    private static final class TestResultCallback extends IAppSearchResultCallback.Stub {
        private final SettableFuture<AppSearchResult<Void>> future = SettableFuture.create();

        @Override
        public void onResult(AppSearchResultParcel appSearchResultParcel) throws RemoteException {
            future.set(appSearchResultParcel.getResult());
        }

        public AppSearchResult<Void> get() throws InterruptedException, ExecutionException {
            return future.get();
        }
    }

    private static final class TestBatchResultErrorCallback extends
            IAppSearchBatchResultCallback.Stub {
        private final SettableFuture<AppSearchResult<Void>> future = SettableFuture.create();

        @Override
        public void onResult(AppSearchBatchResultParcel appSearchBatchResultParcel)
                throws RemoteException {
            future.set(null);
        }

        @Override
        public void onSystemError(AppSearchResultParcel appSearchResultParcel)
                throws RemoteException {
            future.set(appSearchResultParcel.getResult());
        }

        public AppSearchResult<Void> get() throws InterruptedException, ExecutionException {
            return future.get();
        }
    }

    private static final String DATABASE_NAME = "databaseName";
    private static final String NAMESPACE = "namespace";
    private static final String ID = "ID";
    // Mostly guarantees the logged estimated binder latency is positive and doesn't overflow
    private static final long BINDER_CALL_START_TIME = SystemClock.elapsedRealtime() - 1;

    private final MockServiceManager mMockServiceManager = new MockServiceManager();

    @Rule
    public StaticMockFixtureRule mStaticMockFixtureRule = new StaticMockFixtureRule(
            mMockServiceManager, new TestableDeviceConfig());

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private Context mContext;
    private AppSearchManagerService mAppSearchManagerService;
    private UserHandle mUserHandle;
    private UiAutomation mUiAutomation;
    private IAppSearchManager.Stub mAppSearchManagerServiceStub;
    private AppSearchUserInstance mUserInstance;
    private PlatformLogger mPlatformLogger;

    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mUserHandle = context.getUser();
        mUiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        mContext = new ContextWrapper(context) {
            @Override
            public Intent registerReceiverForAllUsers(
                    @Nullable BroadcastReceiver receiver,
                    @NonNull IntentFilter filter,
                    @Nullable String broadcastPermission,
                    @Nullable Handler scheduler) {
                // Do nothing
                return null;
            }
        };

        // Set a test environment that provides a temporary folder for AppSearch
        File mAppSearchDir = mTemporaryFolder.newFolder();
        AppSearchEnvironmentFactory.setEnvironmentInstanceForTest(
                new FrameworkAppSearchEnvironment() {
                    @Override
                    public File getAppSearchDir(@NonNull Context unused,
                            @NonNull UserHandle userHandle) {
                        return mAppSearchDir;
                    }
                });

        // Create the user instance and add a spy to its logger to verify logging
        // Note, SimpleTestLogger does not suffice for our tests since CallStats logging in
        // AppSearchManagerService occurs in a separate thread. With a spy, we can verify with a
        // timeout to catch asynchronous calls.
        AppSearchConfig appSearchConfig = FrameworkAppSearchConfig.create(DIRECT_EXECUTOR);
        mUserInstance = AppSearchUserInstanceManager.getInstance().getOrCreateUserInstance(mContext,
                mUserHandle, appSearchConfig);
        mPlatformLogger = spy(mUserInstance.getLogger());
        mUserInstance.setLoggerForTest(mPlatformLogger);

        // Start the service
        mAppSearchManagerService = new AppSearchManagerService(
                mContext, new AppSearchModule.Lifecycle(mContext));
        mAppSearchManagerService.onStart();
        mAppSearchManagerServiceStub = mMockServiceManager.mStubCaptor.getValue();
        assertThat(mAppSearchManagerServiceStub).isNotNull();
    }

    @After
    public void tearDown() {
        // The TemporaryFolder rule's teardown will delete the current test folder; by removing the
        // current user instance, the next test will be able to create a new AppSearchImpl with
        // a new test folder
        AppSearchUserInstanceManager.getInstance().closeAndRemoveUserInstance(mUserHandle);
    }

    @Test
    public void testCallingPackageDoesntExistsInTargetUser() throws Exception {
        UserHandle testTargetUser = new UserHandle(1234);
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.INTERACT_ACROSS_USERS_FULL);
        try {
            // Try to initial a AppSearchSession for secondary user, but the calling package doesn't
            // exists in there.
            SettableFuture<AppSearchResult<Void>> future = SettableFuture.create();
            mAppSearchManagerServiceStub.initialize(
                    mContext.getAttributionSource(),
                    testTargetUser,
                    System.currentTimeMillis(),
                    new IAppSearchResultCallback.Stub() {
                        @Override
                        public void onResult(AppSearchResultParcel resultParcel) {
                            future.set(resultParcel.getResult());
                        }
                    });
            assertThat(future.get().isSuccess()).isFalse();
            assertThat(future.get().getErrorMessage()).contains(
                    "SecurityException: Package: " + mContext.getPackageName()
                            + " haven't installed for user "
                            + testTargetUser.getIdentifier());
        } finally {
            mUiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void testSetSchemaStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.setSchema(mContext.getAttributionSource(), DATABASE_NAME,
                /* schemaBundles= */ Collections.emptyList(),
                /* visibilityBundles= */ Collections.emptyList(), /* forceOverride= */ false,
                /* schemaVersion= */ 0, mUserHandle, BINDER_CALL_START_TIME,
                SchemaMigrationStats.FIRST_CALL_GET_INCOMPATIBLE, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME, CallStats.CALL_TYPE_SET_SCHEMA);
        // SetSchemaStats is also logged in SetSchema
        ArgumentCaptor<SetSchemaStats> setSchemaStatsCaptor = ArgumentCaptor.forClass(
                SetSchemaStats.class);
        verify(mPlatformLogger, timeout(1000).times(1)).logStats(setSchemaStatsCaptor.capture());
        SetSchemaStats setSchemaStats = setSchemaStatsCaptor.getValue();
        assertThat(setSchemaStats.getPackageName()).isEqualTo(mContext.getPackageName());
        assertThat(setSchemaStats.getDatabase()).isEqualTo(DATABASE_NAME);
        assertThat(setSchemaStats.getStatusCode()).isEqualTo(callback.get().getResultCode());
        assertThat(setSchemaStats.getSchemaMigrationCallType()).isEqualTo(
                SchemaMigrationStats.FIRST_CALL_GET_INCOMPATIBLE);
    }

    @Test
    public void testLocalGetSchemaStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getSchema(mContext.getAttributionSource(),
                mContext.getPackageName(), DATABASE_NAME, mUserHandle, BINDER_CALL_START_TIME,
                callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME, CallStats.CALL_TYPE_GET_SCHEMA);
    }

    @Test
    public void testGlobalGetSchemaStatsLogging() throws Exception {
        String otherPackageName = mContext.getPackageName() + "foo";
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getSchema(mContext.getAttributionSource(),
                otherPackageName, DATABASE_NAME, mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GLOBAL_GET_SCHEMA);
    }

    @Test
    public void testGetNamespacesStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getNamespaces(mContext.getAttributionSource(), DATABASE_NAME,
                mUserHandle, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GET_NAMESPACES);
    }

    @Test
    public void testPutDocumentsStatsLogging() throws Exception {
        TestBatchResultErrorCallback callback = new TestBatchResultErrorCallback();
        mAppSearchManagerServiceStub.putDocuments(mContext.getAttributionSource(), DATABASE_NAME,
                new DocumentsParcel(Collections.emptyList()), mUserHandle, BINDER_CALL_START_TIME,
                callback);
        assertThat(callback.get()).isNull(); // null means there wasn't an error
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_PUT_DOCUMENTS);
        // putDocuments only logs PutDocumentStats indirectly so we don't verify it
    }

    @Test
    public void testLocalGetDocumentsStatsLogging() throws Exception {
        TestBatchResultErrorCallback callback = new TestBatchResultErrorCallback();
        mAppSearchManagerServiceStub.getDocuments(mContext.getAttributionSource(),
                mContext.getPackageName(), DATABASE_NAME, NAMESPACE,
                /* ids= */ Collections.emptyList(), /* typePropertyPaths= */ Collections.emptyMap(),
                mUserHandle, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get()).isNull(); // null means there wasn't an error
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GET_DOCUMENTS);
    }

    @Test
    public void testGlobalGetDocumentsStatsLogging() throws Exception {
        String otherPackageName = mContext.getPackageName() + "foo";
        TestBatchResultErrorCallback callback = new TestBatchResultErrorCallback();
        mAppSearchManagerServiceStub.getDocuments(mContext.getAttributionSource(),
                otherPackageName, DATABASE_NAME, NAMESPACE,
                /* ids= */ Collections.emptyList(), /* typePropertyPaths= */ Collections.emptyMap(),
                mUserHandle, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get()).isNull(); // null means there wasn't an error
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GLOBAL_GET_DOCUMENT_BY_ID);
    }

    @Test
    public void testQueryStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.query(mContext.getAttributionSource(), DATABASE_NAME,
                /* queryExpression= */ "", /* searchSpecBundle= */ new Bundle(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME, CallStats.CALL_TYPE_SEARCH);
        // query only logs SearchStats indirectly so we don't verify it
    }

    @Test
    public void testGlobalQueryStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.globalQuery(mContext.getAttributionSource(),
                /* queryExpression= */ "", /* searchSpecBundle= */ new Bundle(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_GLOBAL_SEARCH);
        // globalQuery only logs SearchStats indirectly so we don't verify it
    }

    @Test
    public void testLocalGetNextPageStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getNextPage(mContext.getAttributionSource(), DATABASE_NAME,
                /* nextPageToken= */ 0,
                AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID, mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GET_NEXT_PAGE);
        // getNextPage also logs SearchStats
        ArgumentCaptor<SearchStats> searchStatsCaptor = ArgumentCaptor.forClass(SearchStats.class);
        verify(mPlatformLogger, timeout(1000).times(1)).logStats(searchStatsCaptor.capture());
        SearchStats searchStats = searchStatsCaptor.getValue();
        assertThat(searchStats.getVisibilityScope()).isEqualTo(SearchStats.VISIBILITY_SCOPE_LOCAL);
        assertThat(searchStats.getPackageName()).isEqualTo(mContext.getPackageName());
        assertThat(searchStats.getDatabase()).isEqualTo(DATABASE_NAME);
        assertThat(searchStats.getJoinType()).isEqualTo(
                AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
    }

    @Test
    public void testGlobalGetNextPageStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getNextPage(mContext.getAttributionSource(),
                /* databaseName= */ null, /* nextPageToken= */ 0,
                AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID, mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_GLOBAL_GET_NEXT_PAGE);
        // getNextPage also logs SearchStats
        ArgumentCaptor<SearchStats> searchStatsCaptor = ArgumentCaptor.forClass(SearchStats.class);
        verify(mPlatformLogger, timeout(1000).times(1)).logStats(searchStatsCaptor.capture());
        SearchStats searchStats = searchStatsCaptor.getValue();
        assertThat(searchStats.getVisibilityScope()).isEqualTo(SearchStats.VISIBILITY_SCOPE_GLOBAL);
        assertThat(searchStats.getPackageName()).isEqualTo(mContext.getPackageName());
        assertThat(searchStats.getDatabase()).isNull();
        assertThat(searchStats.getJoinType()).isEqualTo(
                AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID);
    }

    @Test
    public void testInvalidateNextPageTokenStatsLogging() throws Exception {
        mAppSearchManagerServiceStub.invalidateNextPageToken(mContext.getAttributionSource(),
                /* nextPageToken= */ 0, mUserHandle, BINDER_CALL_START_TIME);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_INVALIDATE_NEXT_PAGE_TOKEN);
    }

    @Test
    public void testWriteQueryResultsToFileStatsLogging() throws Exception {
        File tempFile = mTemporaryFolder.newFile();
        FileDescriptor fd = IoBridge.open(tempFile.getPath(), O_WRONLY);
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.writeQueryResultsToFile(mContext.getAttributionSource(),
                DATABASE_NAME, new ParcelFileDescriptor(fd),
                /* queryExpression= */ "", /* searchSpecBundle= */ new Bundle(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_WRITE_SEARCH_RESULTS_TO_FILE);
    }

    @Test
    public void testPutDocumentsFromFileStatsLogging() throws Exception {
        File tempFile = mTemporaryFolder.newFile();
        FileDescriptor fd = IoBridge.open(tempFile.getPath(), O_RDONLY);
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.putDocumentsFromFile(mContext.getAttributionSource(),
                DATABASE_NAME, new ParcelFileDescriptor(fd), mUserHandle,
                /* schemaMigrationStatsBundle= */ new Bundle(),
                /* totalLatencyStartTimeMillis= */ 0, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_PUT_DOCUMENTS_FROM_FILE);
        // putDocumentsFromFile also logs SchemaMigrationStats
        ArgumentCaptor<SchemaMigrationStats> migrationStatsCaptor =
                ArgumentCaptor.forClass(SchemaMigrationStats.class);
        verify(mPlatformLogger, timeout(1000).times(1)).logStats(migrationStatsCaptor.capture());
        SchemaMigrationStats migrationStats = migrationStatsCaptor.getValue();
        assertThat(migrationStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(migrationStats.getSaveDocumentLatencyMillis()).isGreaterThan(0);
        // putDocumentsFromFile only logs PutDocumentStats indirectly so we don't verify it
    }

    @Test
    public void testSearchSuggestionStatsLogging() throws Exception {
        Bundle searchSuggestionSpecBundle = new Bundle();
        searchSuggestionSpecBundle.putInt("maximumResultCount", 1);
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.searchSuggestion(mContext.getAttributionSource(),
                DATABASE_NAME, /* suggestionQueryExpression= */ "foo", searchSuggestionSpecBundle,
                mUserHandle, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_SEARCH_SUGGESTION);
    }

    @Test
    public void testLocalReportUsageStatsLogging() throws Exception {
        setUpTestSchema(mContext.getPackageName(), DATABASE_NAME);
        setUpTestDocument(mContext.getPackageName(), DATABASE_NAME, NAMESPACE, ID);
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.reportUsage(mContext.getAttributionSource(),
                mContext.getPackageName(), DATABASE_NAME, NAMESPACE, ID,
                /* usageTimestampMillis= */ 0, /* systemUsage= */ false, mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME, CallStats.CALL_TYPE_REPORT_USAGE);
        removeTestSchema(mContext.getPackageName(), DATABASE_NAME);
    }

    @Test
    public void testGlobalReportUsageStatsLogging() throws Exception {
        // Grant system access for global report usage
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.READ_GLOBAL_APP_SEARCH_DATA);
        try {
            String otherPackageName = mContext.getPackageName() + "foo";
            setUpTestSchema(otherPackageName, DATABASE_NAME);
            setUpTestDocument(otherPackageName, DATABASE_NAME, NAMESPACE, ID);
            TestResultCallback callback = new TestResultCallback();
            mAppSearchManagerServiceStub.reportUsage(mContext.getAttributionSource(),
                    otherPackageName, DATABASE_NAME, NAMESPACE, ID, /* usageTimestampMillis= */ 0,
                    /* systemUsage= */ true, mUserHandle, BINDER_CALL_START_TIME, callback);
            assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
            verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                    CallStats.CALL_TYPE_REPORT_SYSTEM_USAGE);
            removeTestSchema(otherPackageName, DATABASE_NAME);
        } finally {
            mUiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void testRemoveByDocumentIdStatsLogging() throws Exception {
        TestBatchResultErrorCallback callback = new TestBatchResultErrorCallback();
        mAppSearchManagerServiceStub.removeByDocumentId(mContext.getAttributionSource(),
                DATABASE_NAME, NAMESPACE, /* ids= */ Collections.emptyList(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get()).isNull(); // null means there wasn't an error
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_REMOVE_DOCUMENTS_BY_ID);
    }

    @Test
    public void testRemoveByQueryStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.removeByQuery(mContext.getAttributionSource(), DATABASE_NAME,
                /* queryExpression= */ "", /* searchSpecBundle= */ new Bundle(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_REMOVE_DOCUMENTS_BY_SEARCH);
    }

    @Test
    public void testGetStorageInfoStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.getStorageInfo(mContext.getAttributionSource(), DATABASE_NAME,
                mUserHandle, BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), DATABASE_NAME,
                CallStats.CALL_TYPE_GET_STORAGE_INFO);
    }

    @Test
    public void testPersistToDiskStatsLogging() throws Exception {
        mAppSearchManagerServiceStub.persistToDisk(mContext.getAttributionSource(), mUserHandle,
                BINDER_CALL_START_TIME);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_FLUSH);
    }

    @Test
    public void testRegisterObserverCallbackStatsLogging() throws Exception {
        AppSearchResultParcel<Void> resultParcel =
                mAppSearchManagerServiceStub.registerObserverCallback(
                        mContext.getAttributionSource(), mContext.getPackageName(),
                        /* observerSpecBundle= */ new Bundle(), mUserHandle, BINDER_CALL_START_TIME,
                        new IAppSearchObserverProxy.Stub() {
                            @Override
                            public void onSchemaChanged(String packageName, String databaseName,
                                    List<String> changedSchemaNames) throws RemoteException {
                            }

                            @Override
                            public void onDocumentChanged(String packageName, String databaseName,
                                    String namespace, String schemaName,
                                    List<String> changedDocumentIds) throws RemoteException {
                            }
                        });
        assertThat(resultParcel.getResult().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_REGISTER_OBSERVER_CALLBACK);
    }

    @Test
    public void testUnregisterObserverCallbackStatsLogging() throws Exception {
        AppSearchResultParcel<Void> resultParcel =
                mAppSearchManagerServiceStub.unregisterObserverCallback(
                        mContext.getAttributionSource(), mContext.getPackageName(), mUserHandle,
                        BINDER_CALL_START_TIME, new IAppSearchObserverProxy.Stub() {
                            @Override
                            public void onSchemaChanged(String packageName, String databaseName,
                                    List<String> changedSchemaNames) throws RemoteException {
                            }

                            @Override
                            public void onDocumentChanged(String packageName, String databaseName,
                                    String namespace, String schemaName,
                                    List<String> changedDocumentIds) throws RemoteException {
                            }
                        });
        assertThat(resultParcel.getResult().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(),
                CallStats.CALL_TYPE_UNREGISTER_OBSERVER_CALLBACK);
    }

    @Test
    public void testInitializeStatsLogging() throws Exception {
        TestResultCallback callback = new TestResultCallback();
        mAppSearchManagerServiceStub.initialize(mContext.getAttributionSource(), mUserHandle,
                BINDER_CALL_START_TIME, callback);
        assertThat(callback.get().getResultCode()).isEqualTo(AppSearchResult.RESULT_OK);
        verifyCallStats(mContext.getPackageName(), CallStats.CALL_TYPE_INITIALIZE);
        // initialize only logs InitializeStats indirectly so we don't verify it
    }

    private void verifyCallStats(String packageName, String databaseName, int callType) {
        ArgumentCaptor<CallStats> captor = ArgumentCaptor.forClass(CallStats.class);
        verify(mPlatformLogger, timeout(1000).times(1)).logStats(captor.capture());
        CallStats callStats = captor.getValue();
        assertThat(callStats.getPackageName()).isEqualTo(packageName);
        assertThat(callStats.getDatabase()).isEqualTo(databaseName);
        assertThat(callStats.getCallType()).isEqualTo(callType);
        assertThat(callStats.getStatusCode()).isEqualTo(AppSearchResult.RESULT_OK);
        assertThat(callStats.getEstimatedBinderLatencyMillis()).isGreaterThan(0);
    }

    private void verifyCallStats(String packageName, int callType) {
        verifyCallStats(packageName, /* databaseName= */ null, callType);
    }

    private void setUpTestSchema(String packageName, String databaseName) throws Exception {
        // Insert schema
        List<AppSearchSchema> schemas = Collections.singletonList(
                new AppSearchSchema.Builder("type").build());
        InternalSetSchemaResponse internalSetSchemaResponse =
                mUserInstance.getAppSearchImpl().setSchema(packageName, databaseName, schemas,
                        /* visibilityDocuments= */ Collections.emptyList(),
                        /* forceOverride= */ false,
                        /* version= */ 0,
                        /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
    }

    private void setUpTestDocument(String packageName, String databaseName, String namespace,
            String id) throws Exception {
        // Insert a document
        GenericDocument document = new GenericDocument.Builder<>(namespace, id, "type").build();
        mUserInstance.getAppSearchImpl().putDocument(packageName, databaseName, document,
                /* sendChangeNotifications= */ false,
                /* logger= */ null);
    }

    private void removeTestSchema(String packageName, String databaseName) throws Exception {
        InternalSetSchemaResponse internalSetSchemaResponse =
                mUserInstance.getAppSearchImpl().setSchema(packageName, databaseName,
                        /* schemas= */ Collections.emptyList(),
                        /* visibilityDocuments= */ Collections.emptyList(),
                        /* forceOverride= */ true,
                        /* version= */ 0,
                        /* setSchemaStatsBuilder= */ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
    }

    private static class MockServiceManager implements StaticMockFixture {
        ArgumentCaptor<IAppSearchManager.Stub> mStubCaptor =
                ArgumentCaptor.forClass(IAppSearchManager.Stub.class);

        @Override
        public StaticMockitoSessionBuilder setUpMockedClasses(
                @NonNull StaticMockitoSessionBuilder sessionBuilder) {
            sessionBuilder.mockStatic(LocalManagerRegistry.class);
            sessionBuilder.spyStatic(ServiceManager.class);
            return sessionBuilder;
        }

        @Override
        public void setUpMockBehaviors() {
            ExtendedMockito.doReturn(mock(StorageStatsManagerLocal.class))
                    .when(() -> LocalManagerRegistry.getManager(StorageStatsManagerLocal.class));
            ExtendedMockito.doNothing().when(
                    () -> ServiceManager.addService(
                            anyString(), mStubCaptor.capture(), anyBoolean(), anyInt()));
        }

        @Override
        public void tearDown() {
        }
    }
}
