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

package android.app.appsearch;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.appsearch.safeparcel.AbstractSafeParcelable;
import android.app.appsearch.safeparcel.SafeParcelable;
import android.os.Parcel;
import android.os.Parcelable;

import com.android.appsearch.flags.Flags;

import java.util.Arrays;
import java.util.Objects;

/**
 * Embeddings are vector representations of data, such as text, images, and audio, which can be
 * generated by machine learning models and used for semantic search. This class represents an
 * embedding vector, which wraps a float array for the values of the embedding vector and a model
 * signature that can be any string to distinguish between embedding vectors generated by different
 * models.
 *
 * <p>For more details on how embedding search works, check {@link AppSearchSession#search} and
 * {@link SearchSpec.Builder#setRankingStrategy(String)}.
 *
 * @see SearchSpec.Builder#addSearchEmbeddings
 * @see GenericDocument.Builder#setPropertyEmbedding
 */
@FlaggedApi(Flags.FLAG_ENABLE_SCHEMA_EMBEDDING_PROPERTY_CONFIG)
@SafeParcelable.Class(creator = "EmbeddingVectorCreator")
@SuppressWarnings("HiddenSuperclass")
public final class EmbeddingVector extends AbstractSafeParcelable {

    @NonNull
    public static final Parcelable.Creator<EmbeddingVector> CREATOR = new EmbeddingVectorCreator();

    @NonNull
    @Field(id = 1, getter = "getValues")
    private final float[] mValues;

    @NonNull
    @Field(id = 2, getter = "getModelSignature")
    private final String mModelSignature;

    @Nullable private Integer mHashCode;

    /**
     * Creates a new {@link EmbeddingVector}.
     *
     * @throws IllegalArgumentException if {@code values} is empty.
     */
    @Constructor
    public EmbeddingVector(
            @Param(id = 1) @NonNull float[] values, @Param(id = 2) @NonNull String modelSignature) {
        mValues = Objects.requireNonNull(values);
        if (mValues.length == 0) {
            throw new IllegalArgumentException("Embedding values cannot be empty.");
        }
        mModelSignature = Objects.requireNonNull(modelSignature);
    }

    /** Returns the values of this embedding vector. */
    @NonNull
    public float[] getValues() {
        return mValues;
    }

    /**
     * Returns the model signature of this embedding vector, which is an arbitrary string to
     * distinguish between embedding vectors generated by different models.
     */
    @NonNull
    public String getModelSignature() {
        return mModelSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof EmbeddingVector)) return false;
        EmbeddingVector that = (EmbeddingVector) o;
        return Arrays.equals(mValues, that.mValues) && mModelSignature.equals(that.mModelSignature);
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = Objects.hash(Arrays.hashCode(mValues), mModelSignature);
        }
        return mHashCode;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        EmbeddingVectorCreator.writeToParcel(this, dest, flags);
    }
}