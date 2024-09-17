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

package android.app.appsearch.aidl;

import static android.app.appsearch.testutil.AppSearchTestUtils.calculateDigest;
import static android.app.appsearch.testutil.AppSearchTestUtils.generateRandomBytes;
import static android.os.ParcelFileDescriptor.MODE_WRITE_ONLY;

import static com.google.common.truth.Truth.assertThat;

import android.app.appsearch.AppSearchBatchResult;
import android.app.appsearch.AppSearchBlobHandle;
import android.app.appsearch.AppSearchResult;
import android.os.ParcelFileDescriptor;

import org.junit.Test;

import java.io.File;

public class AppSearchBatchResultGeneralKeyParcelTest {

    // TODO(b/273591938) remove label and use namespace to create blob handle when sync support blob
    //  namespace to the framework.
    @Test
    public void testFromBlobHandleToPfd() throws Exception {
        File file = File.createTempFile(/* prefix= */ "appsearch", /* suffix= */ null);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, MODE_WRITE_ONLY);
        AppSearchResult<ParcelFileDescriptor> successResult =
                AppSearchResult.newSuccessfulResult(pfd);
        AppSearchResult<ParcelFileDescriptor> failureResult =
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_NOT_FOUND, "not found");
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] data2 = generateRandomBytes(10); // 10 Bytes
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(digest1, "label123");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(digest2, "label123");
        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> result =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, ParcelFileDescriptor>()
                        .setResult(blobHandle1, successResult)
                        .setResult(blobHandle2, failureResult)
                        .build();
        AppSearchBatchResultGeneralKeyParcel<AppSearchBlobHandle, ParcelFileDescriptor>
                resultParcel = AppSearchBatchResultGeneralKeyParcel.fromBlobHandleToPfd(result);

        AppSearchBatchResult<AppSearchBlobHandle, ParcelFileDescriptor> outResult =
                resultParcel.getResult();

        assertThat(outResult.getSuccesses()).containsExactly(blobHandle1, pfd);
        assertThat(outResult.getFailures()).containsExactly(blobHandle2, failureResult);
    }

    @Test
    public void testFromBlobHandleToVoid() throws Exception {
        byte[] data1 = generateRandomBytes(10); // 10 Bytes
        byte[] digest1 = calculateDigest(data1);
        byte[] data2 = generateRandomBytes(10); // 10 Bytes
        byte[] digest2 = calculateDigest(data2);
        AppSearchBlobHandle blobHandle1 = AppSearchBlobHandle.createWithSha256(digest1, "label123");
        AppSearchBlobHandle blobHandle2 = AppSearchBlobHandle.createWithSha256(digest2, "label123");
        AppSearchResult<Void> successResult = AppSearchResult.newSuccessfulResult(null);
        AppSearchResult<Void> failureResult =
                AppSearchResult.newFailedResult(AppSearchResult.RESULT_NOT_FOUND, "not found");
        AppSearchBatchResult<AppSearchBlobHandle, Void> result =
                new AppSearchBatchResult.Builder<AppSearchBlobHandle, Void>()
                        .setResult(blobHandle1, successResult)
                        .setResult(blobHandle2, failureResult)
                        .build();
        AppSearchBatchResultGeneralKeyParcel<AppSearchBlobHandle, Void> resultParcel =
                AppSearchBatchResultGeneralKeyParcel.fromBlobHandleToVoid(result);

        AppSearchBatchResult<AppSearchBlobHandle, Void> outResult = resultParcel.getResult();

        assertThat(outResult.getSuccesses()).containsExactly(blobHandle1, null);
        assertThat(outResult.getFailures()).containsExactly(blobHandle2, failureResult);
    }
}
