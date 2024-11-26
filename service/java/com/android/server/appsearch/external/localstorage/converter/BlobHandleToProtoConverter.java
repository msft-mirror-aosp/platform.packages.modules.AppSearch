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

package com.android.server.appsearch.external.localstorage.converter;

import android.annotation.NonNull;
import android.app.appsearch.AppSearchBlobHandle;
import android.app.appsearch.exceptions.AppSearchException;

import com.android.server.appsearch.external.localstorage.util.PrefixUtil;

import com.google.android.icing.proto.PropertyProto;
import com.google.protobuf.ByteString;

/**
 * Translates a {@link android.app.blob.BlobHandle} into {@link PropertyProto.BlobHandleProto}.
 *
 * @hide
 */
public final class BlobHandleToProtoConverter {
    private BlobHandleToProtoConverter() {}

    /** Converters a {@link AppSearchBlobHandle} into {@link PropertyProto.BlobHandleProto}. */
    @NonNull
    public static PropertyProto.BlobHandleProto toBlobHandleProto(
            @NonNull AppSearchBlobHandle blobHandle) {
        return PropertyProto.BlobHandleProto.newBuilder()
                .setNamespace(
                        PrefixUtil.createPrefix(
                                        blobHandle.getPackageName(), blobHandle.getDatabaseName())
                                + blobHandle.getNamespace())
                .setDigest(ByteString.copyFrom(blobHandle.getSha256Digest()))
                .build();
    }

    /** Converters a {@link PropertyProto.BlobHandleProto} into {@link AppSearchBlobHandle}. */
    @NonNull
    public static AppSearchBlobHandle toAppSearchBlobHandle(
            @NonNull PropertyProto.BlobHandleProto proto) throws AppSearchException {
        String prefix = PrefixUtil.getPrefix(proto.getNamespace());
        return AppSearchBlobHandle.createWithSha256(
                proto.getDigest().toByteArray(),
                PrefixUtil.getPackageName(prefix),
                PrefixUtil.getDatabaseName(prefix),
                PrefixUtil.removePrefix(proto.getNamespace()));
    }
}
