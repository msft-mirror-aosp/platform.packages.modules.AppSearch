/*
 * Copyright 2020 The Android Open Source Project
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
import android.app.appsearch.annotation.CanIgnoreReturnValue;
import android.app.appsearch.exceptions.AppSearchException;
import android.app.appsearch.flags.Flags;
import android.util.ArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Encapsulates a request to index documents into an {@link AppSearchSession} database.
 *
 * @see AppSearchSession#put
 */
public final class PutDocumentsRequest {
    private final List<GenericDocument> mDocuments;

    private final List<GenericDocument> mTakenActions;

    PutDocumentsRequest(List<GenericDocument> documents, List<GenericDocument> takenActions) {
        mDocuments = documents;
        mTakenActions = takenActions;
    }

    /** Returns a list of {@link GenericDocument} objects that are part of this request. */
    @NonNull
    public List<GenericDocument> getGenericDocuments() {
        return Collections.unmodifiableList(mDocuments);
    }

    /**
     * Returns a list of {@link GenericDocument} objects containing taken action metrics that are
     * part of this request.
     *
     * <p>See {@link Builder#addTakenActionGenericDocuments(GenericDocument...)}.
     */
    @NonNull
    @FlaggedApi(Flags.FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS)
    public List<GenericDocument> getTakenActionGenericDocuments() {
        return Collections.unmodifiableList(mTakenActions);
    }

    /** Builder for {@link PutDocumentsRequest} objects. */
    public static final class Builder {
        private ArrayList<GenericDocument> mDocuments = new ArrayList<>();
        private ArrayList<GenericDocument> mTakenActions = new ArrayList<>();
        private boolean mBuilt = false;

        /** Adds one or more {@link GenericDocument} objects to the request. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addGenericDocuments(@NonNull GenericDocument... documents) {
            Objects.requireNonNull(documents);
            resetIfBuilt();
            return addGenericDocuments(Arrays.asList(documents));
        }

        /** Adds a collection of {@link GenericDocument} objects to the request. */
        @CanIgnoreReturnValue
        @NonNull
        public Builder addGenericDocuments(
                @NonNull Collection<? extends GenericDocument> documents) {
            Objects.requireNonNull(documents);
            resetIfBuilt();
            mDocuments.addAll(documents);
            return this;
        }

        /**
         * Adds one or more {@link GenericDocument} objects containing taken action metrics to the
         * request.
         *
         * <p>Metrics to be collected by AppSearch:
         *
         * <ul>
         *   <li>name: STRING, the name of the taken action.
         *       <p>Name is an optional custom field that allows the client to tag and categorize
         *       taken action {@link GenericDocument}.
         *   <li>referencedQualifiedId: STRING, the qualified id of the {@link SearchResult}
         *       document that the user takes action on.
         *       <p>A qualified id is a string generated by package, database, namespace, and
         *       document id. See {@link
         *       android.app.appsearch.util.DocumentIdUtil#createQualifiedId} for more details.
         *   <li>previousQueries: REPEATED STRING, the list of all previous user-entered search
         *       inputs, without any operators or rewriting, collected during this search session in
         *       chronological order.
         *   <li>finalQuery: STRING, the final user-entered search input (without any operators or
         *       rewriting) that yielded the {@link SearchResult} on which the user took action.
         *   <li>resultRankInBlock: LONG, the rank of the {@link SearchResult} document among the
         *       user-defined block.
         *       <p>The client can define its own custom definition for block, e.g. corpus name,
         *       group, etc.
         *       <p>For example, a client defines the block as corpus, and AppSearch returns 5
         *       documents with corpus = ["corpus1", "corpus1", "corpus2", "corpus3", "corpus2"].
         *       Then the block ranks of them = [1, 2, 1, 1, 2].
         *       <p>If the client is not presenting the results in multiple blocks, they should set
         *       this value to match resultRankGlobal.
         *   <li>resultRankGlobal: LONG, the global rank of the {@link SearchResult} document.
         *       <p>Global rank reflects the order of {@link SearchResult} documents returned by
         *       AppSearch.
         *       <p>For example, AppSearch returns 2 pages with 10 {@link SearchResult} documents
         *       for each page. Then the global ranks of them will be 1 to 10 for the first page,
         *       and 11 to 20 for the second page.
         *   <li>timeStayOnResultMillis: LONG, the time in milliseconds that user stays on the
         *       {@link SearchResult} document after clicking it.
         * </ul>
         *
         * <p>Certain anonymized information about actions reported using this API may be uploaded
         * using statsd and may be used to improve the quality of the search algorithms. Most of the
         * information in this class is already non-identifiable, such as durations and its position
         * in the result set. Identifiable information which you choose to provide, such as the
         * query string, will be anonymized using techniques like Federated Analytics to ensure only
         * the most frequently searched terms across the whole user population are retained and
         * available for study.
         *
         * <p>You can alternatively use the {@link #addGenericDocuments(GenericDocument...)} API to
         * retain the benefits of joining and using it on-device, without triggering any of the
         * anonymized stats uploading described above.
         *
         * @param takenActionGenericDocuments one or more {@link GenericDocument} objects containing
         *     taken action metric fields.
         */
        @CanIgnoreReturnValue
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS)
        public Builder addTakenActionGenericDocuments(
                @NonNull GenericDocument... takenActionGenericDocuments) throws AppSearchException {
            Objects.requireNonNull(takenActionGenericDocuments);
            resetIfBuilt();
            return addTakenActionGenericDocuments(Arrays.asList(takenActionGenericDocuments));
        }

        /**
         * Adds a collection of {@link GenericDocument} objects containing taken action metrics to
         * the request.
         *
         * @see #addTakenActionGenericDocuments(GenericDocument...)
         * @param takenActionGenericDocuments a collection of {@link GenericDocument} objects
         *     containing taken action metric fields.
         */
        @CanIgnoreReturnValue
        @NonNull
        @FlaggedApi(Flags.FLAG_ENABLE_PUT_DOCUMENTS_REQUEST_ADD_TAKEN_ACTIONS)
        public Builder addTakenActionGenericDocuments(
                @NonNull Collection<? extends GenericDocument> takenActionGenericDocuments)
                throws AppSearchException {
            Objects.requireNonNull(takenActionGenericDocuments);
            resetIfBuilt();
            mTakenActions.addAll(takenActionGenericDocuments);
            return this;
        }

        /**
         * Creates a new {@link PutDocumentsRequest} object.
         *
         * @throws IllegalArgumentException if there is any id collision between normal and action
         *     documents.
         */
        @NonNull
        public PutDocumentsRequest build() {
            mBuilt = true;

            // Verify there is no id collision between normal documents and action documents.
            Set<String> idSet = new ArraySet<>();
            for (int i = 0; i < mDocuments.size(); i++) {
                idSet.add(mDocuments.get(i).getId());
            }
            for (int i = 0; i < mTakenActions.size(); i++) {
                GenericDocument takenAction = mTakenActions.get(i);
                if (idSet.contains(takenAction.getId())) {
                    throw new IllegalArgumentException(
                            "Document id "
                                    + takenAction.getId()
                                    + " cannot exist in both taken action and normal document");
                }
            }

            return new PutDocumentsRequest(mDocuments, mTakenActions);
        }

        private void resetIfBuilt() {
            if (mBuilt) {
                mDocuments = new ArrayList<>(mDocuments);
                mTakenActions = new ArrayList<>(mTakenActions);
                mBuilt = false;
            }
        }
    }
}
