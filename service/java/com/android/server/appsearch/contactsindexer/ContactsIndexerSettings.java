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

package com.android.server.appsearch.contactsindexer;

import android.annotation.NonNull;
import android.os.PersistableBundle;

import com.android.server.appsearch.indexer.IndexerSettings;

import java.io.File;
import java.util.Objects;

/**
 * Contacts indexer settings backed by a PersistableBundle.
 *
 * <p>Holds settings such as:
 *
 * <ul>
 *   <li>the last time a full update was performed
 *   <li>the last time a delta update was performed
 *   <li>the time of the last CP2 contact update
 *   <li>the time of the last CP2 contact deletion
 * </ul>
 *
 * <p>This class is NOT thread safe (similar to {@link PersistableBundle} which it wraps).
 *
 * @hide
 */
public class ContactsIndexerSettings extends IndexerSettings {

    private static final String TAG = "ContactsIndexerSettings";

    static final String SETTINGS_FILE_NAME = "contacts_indexer_settings.pb";
    static final String LAST_FULL_UPDATE_TIMESTAMP_KEY = "last_full_update_timestamp_millis";
    static final String LAST_DELTA_UPDATE_TIMESTAMP_KEY = "last_delta_update_timestamp_key";
    // TODO(b/296078517): rename the keys to match the constants but keep backwards compatibility
    // Note this constant was renamed from LAST_DELTA_UPDATE_TIMESTAMP_KEY but the key itself has
    // been kept the same for backwards compatibility
    static final String LAST_CONTACT_UPDATE_TIMESTAMP_KEY = "last_delta_update_timestamp_millis";
    // Note this constant was renamed from LAST_DELTA_DELETE_TIMESTAMP_KEY but the key itself has
    // been kept the same for backwards compatibility
    static final String LAST_CONTACT_DELETE_TIMESTAMP_KEY = "last_delta_delete_timestamp_millis";

    public ContactsIndexerSettings(@NonNull File baseDir) {
        super(Objects.requireNonNull(baseDir));
    }

    @Override
    protected String getSettingsFileName() {
        return SETTINGS_FILE_NAME;
    }

    /** Returns the timestamp of when the last full update occurred in milliseconds. */
    public long getLastFullUpdateTimestampMillis() {
        return mBundle.getLong(LAST_FULL_UPDATE_TIMESTAMP_KEY);
    }

    /** Sets the timestamp of when the last full update occurred in milliseconds. */
    public void setLastFullUpdateTimestampMillis(long timestampMillis) {
        mBundle.putLong(LAST_FULL_UPDATE_TIMESTAMP_KEY, timestampMillis);
    }

    /** Returns the timestamp of when the last delta update occurred in milliseconds. */
    public long getLastDeltaUpdateTimestampMillis() {
        return mBundle.getLong(LAST_DELTA_UPDATE_TIMESTAMP_KEY);
    }

    /** Sets the timestamp of when the last delta update occurred in milliseconds. */
    public void setLastDeltaUpdateTimestampMillis(long timestampMillis) {
        mBundle.putLong(LAST_DELTA_UPDATE_TIMESTAMP_KEY, timestampMillis);
    }

    /** Returns the timestamp of when the last contact in CP2 was updated in milliseconds. */
    public long getLastContactUpdateTimestampMillis() {
        return mBundle.getLong(LAST_CONTACT_UPDATE_TIMESTAMP_KEY);
    }

    /** Sets the timestamp of when the last contact in CP2 was updated in milliseconds. */
    public void setLastContactUpdateTimestampMillis(long timestampMillis) {
        mBundle.putLong(LAST_CONTACT_UPDATE_TIMESTAMP_KEY, timestampMillis);
    }

    /** Returns the timestamp of when the last contact in CP2 was deleted in milliseconds. */
    public long getLastContactDeleteTimestampMillis() {
        return mBundle.getLong(LAST_CONTACT_DELETE_TIMESTAMP_KEY);
    }

    /** Sets the timestamp of when the last contact in CP2 was deleted in milliseconds. */
    public void setLastContactDeleteTimestampMillis(long timestampMillis) {
        mBundle.putLong(LAST_CONTACT_DELETE_TIMESTAMP_KEY, timestampMillis);
    }

    /** Resets all the settings to default values. */
    @Override
    public void reset() {
        setLastFullUpdateTimestampMillis(0);
        setLastDeltaUpdateTimestampMillis(0);
        setLastContactUpdateTimestampMillis(0);
        setLastContactDeleteTimestampMillis(0);
    }
}
