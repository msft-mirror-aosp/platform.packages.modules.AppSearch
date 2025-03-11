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

package com.android.server.appsearch;

import static android.text.format.DateUtils.DAY_IN_MILLIS;

import com.android.server.appsearch.external.localstorage.AppSearchConfig;

/**
 * An interface which exposes config flags to AppSearch.
 *
 * <p>This interface provides an abstraction for the AppSearch's flag mechanism and implements
 * caching to avoid expensive lookups. This interface is only used by environments which have a
 * running AppSearch service like Framework and GMSCore. JetPack uses {@link AppSearchConfig}
 * directly instead.
 *
 * <p>Implementations of this interface must be thread-safe.
 *
 * @hide
 */
public interface ServiceAppSearchConfig extends AppSearchConfig, AutoCloseable {
    /**
     * Default min time interval between samples in millis if there is no value set for {@link
     * #getCachedMinTimeIntervalBetweenSamplesMillis()} in the flag system.
     */
    long DEFAULT_MIN_TIME_INTERVAL_BETWEEN_SAMPLES_MILLIS = 50;

    /**
     * Default sampling interval if there is no value set for {@link
     * #getCachedSamplingIntervalDefault()} in the flag system.
     */
    int DEFAULT_SAMPLING_INTERVAL = 10;

    int DEFAULT_LIMIT_CONFIG_MAX_DOCUMENT_SIZE_BYTES = 512 * 1024; // 512KiB
    int DEFAULT_LIMIT_CONFIG_PER_PACKAGE_DOCUMENT_COUNT_LIMIT = 80_000;
    int DEFAULT_LIMIT_CONFIG_DOCUMENT_COUNT_LIMIT_START_THRESHOLD = 2_000_000;
    int DEFAULT_LIMIT_CONFIG_MAX_SUGGESTION_COUNT = 20_000;
    int DEFAULT_BYTES_OPTIMIZE_THRESHOLD = 10 * 1024 * 1024; // 10 MiB
    int DEFAULT_TIME_OPTIMIZE_THRESHOLD_MILLIS = 7 * 24 * 60 * 60 * 1000; // 7 days in millis
    int DEFAULT_DOC_COUNT_OPTIMIZE_THRESHOLD = 10_000;
    int DEFAULT_MIN_TIME_OPTIMIZE_THRESHOLD_MILLIS = 0;
    // Cached API Call Stats is disabled by default
    int DEFAULT_API_CALL_STATS_LIMIT = 0;
    boolean DEFAULT_RATE_LIMIT_ENABLED = false;

    /** This defines the task queue's total capacity for rate limiting. */
    int DEFAULT_RATE_LIMIT_TASK_QUEUE_TOTAL_CAPACITY = Integer.MAX_VALUE;

    /**
     * This defines the per-package capacity for rate limiting as a percentage of the total
     * capacity.
     */
    float DEFAULT_RATE_LIMIT_TASK_QUEUE_PER_PACKAGE_CAPACITY_PERCENTAGE = 1;

    /**
     * This defines API costs used for AppSearch's task queue rate limit.
     *
     * <p>Each entry in the string should follow the format 'api_name:integer_cost', and each entry
     * should be separated by a semi-colon. API names should follow the string definitions in {@link
     * com.android.server.appsearch.external.localstorage.stats.CallStats}.
     *
     * <p>e.g. A valid string: "localPutDocuments:5;localSearch:1;localSetSchema:10"
     */
    String DEFAULT_RATE_LIMIT_API_COSTS_STRING = "";

    boolean DEFAULT_ICING_CONFIG_USE_READ_ONLY_SEARCH = true;
    boolean DEFAULT_USE_FIXED_EXECUTOR_SERVICE = false;
    long DEFAULT_APP_FUNCTION_CALL_TIMEOUT_MILLIS = 30_000;

    /** This flag value is true by default because the flag is intended as a kill-switch. */
    boolean DEFAULT_SHOULD_RETRIEVE_PARENT_INFO = true;

    /** The default interval in millisecond to trigger fully persist job. */
    long DEFAULT_FULLY_PERSIST_JOB_INTERVAL = DAY_IN_MILLIS;

    /**
     * The default number of active fds an app is allowed to open for read and write blob from
     * AppSearch.
     */
    int DEFAULT_MAX_OPEN_BLOB_COUNT = 250;

    /** Returns cached value for minTimeIntervalBetweenSamplesMillis. */
    long getCachedMinTimeIntervalBetweenSamplesMillis();

    /**
     * Returns cached value for default sampling interval for all the stats NOT listed in the
     * configuration.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalDefault();

    /**
     * Returns cached value for sampling interval for batch calls.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForBatchCallStats();

    /**
     * Returns cached value for sampling interval for putDocument.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForPutDocumentStats();

    /**
     * Returns cached value for sampling interval for initialize.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForInitializeStats();

    /**
     * Returns cached value for sampling interval for search.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForSearchStats();

    /**
     * Returns cached value for sampling interval for globalSearch.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForGlobalSearchStats();

    /**
     * Returns cached value for sampling interval for optimize.
     *
     * <p>For example, sampling_interval=10 means that one out of every 10 stats was logged.
     */
    int getCachedSamplingIntervalForOptimizeStats();

    /**
     * Returns the cached optimize byte size threshold.
     *
     * <p>An AppSearch Optimize job will be triggered if the bytes size of garbage resource exceeds
     * this threshold.
     */
    int getCachedBytesOptimizeThreshold();

    /**
     * Returns the cached optimize time interval threshold.
     *
     * <p>An AppSearch Optimize job will be triggered if the time since last optimize job exceeds
     * this threshold.
     */
    int getCachedTimeOptimizeThresholdMs();

    /**
     * Returns the cached optimize document count threshold.
     *
     * <p>An AppSearch Optimize job will be triggered if the number of document of garbage resource
     * exceeds this threshold.
     */
    int getCachedDocCountOptimizeThreshold();

    /**
     * Returns the cached minimum optimize time interval threshold.
     *
     * <p>An AppSearch Optimize job will only be triggered if the time since last optimize job
     * exceeds this threshold.
     */
    int getCachedMinTimeOptimizeThresholdMs();

    /** Returns the maximum number of last API calls' statistics that can be included in dumpsys. */
    int getCachedApiCallStatsLimit();

    /** Returns the cached denylist. */
    Denylist getCachedDenylist();

    /** Returns whether to enable AppSearch rate limiting. */
    boolean getCachedRateLimitEnabled();

    /** Returns the cached {@link AppSearchRateLimitConfig}. */
    AppSearchRateLimitConfig getCachedRateLimitConfig();

    /**
     * Returns the maximum allowed duration for an app function call in milliseconds.
     *
     * @see android.app.appsearch.functions.AppFunctionManager#executeAppFunction
     */
    long getAppFunctionCallTimeoutMillis();

    /**
     * Returns the time interval to schedule a full persist to disk back ground job in milliseconds.
     */
    long getCachedFullyPersistJobIntervalMillis();

    /**
     * Closes this {@link AppSearchConfig}.
     *
     * <p>This close() operation does not throw an exception.
     */
    @Override
    void close();
}
