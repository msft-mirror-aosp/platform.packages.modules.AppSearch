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

package com.android.server.appsearch.util;

import android.annotation.BinderThread;
import android.annotation.NonNull;
import android.util.ArrayMap;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.appsearch.AppSearchRateLimitConfig;
import com.android.server.appsearch.external.localstorage.stats.CallStats;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Contains AppSearch's per-user ExecutorService, and other detailed queue stats related to the
 * user's executor's task queue.
 *
 * <p>This class is @NotThreadSafe. Callers must make sure that call to methods that mutate the
 * ExecutorServiceTaskCounter's state are protected with a lock.
 */
public class ExecutorServiceTaskCounter {
    private static final String TAG = "AppSearchTaskCounter";

    private final ExecutorService mExecutorService;

    /**
     * A map of packageName -> package task count and cost currently on task queue.
     */
    private final ArrayMap<String, TaskCostInfo> mPerPackageTaskCosts = new ArrayMap<>();

    /**
     * Keeps track of the task queue size.
     */
    private int mTaskQueueSize;

    /**
     * Sum of costs of all tasks currently on the executor queue.
     */
    private int mTaskQueueTotalCost;

    public ExecutorServiceTaskCounter(@NonNull ExecutorService executorService) {
        mExecutorService = Objects.requireNonNull(executorService);
        mTaskQueueSize = 0;
    }

    /**
     * Returns true and adds a task to the executor queue by incrementing the count for the task's
     * package and api type, if allowed. It is up to the caller to call this method at the
     * correct time to keep counts in sync with the task queue size.
     *
     * <p>i.e. This should only be called when a task needs to be added to the executor queue.
     */
    @BinderThread
    public boolean addTaskToQueue(@NonNull AppSearchRateLimitConfig rateLimitConfig,
            @NonNull String packageName, @CallStats.CallType int apiType) {
        Objects.requireNonNull(rateLimitConfig);
        Objects.requireNonNull(packageName);

        TaskCostInfo packageTaskCountPair = mPerPackageTaskCosts.get(packageName);
        int totalPackageApiCost =
                packageTaskCountPair == null ? 0 : packageTaskCountPair.mTotalTaskCost;
        int apiCost = rateLimitConfig.getApiCost(apiType);
        if (totalPackageApiCost + apiCost > rateLimitConfig.getTaskQueuePerPackageCapacity() ||
                mTaskQueueTotalCost + apiCost > rateLimitConfig.getTaskQueueTotalCapacity()) {
            return false;
        } else {
            ++mTaskQueueSize;
            mTaskQueueTotalCost += apiCost;
            addPackageTaskInfo(packageName, apiType, apiCost);
            return true;
        }
    }

    /**
     * Removes a task from the executor queue by decrementing the count for the task's package
     * and api type. It is up to the caller to call this method at the correct time to keep counts
     * in sync with the task queue size.
     *
     * <p>i.e. This should only be called after a task has finished executing and has been removed
     * from the executor queue.
     */
    public void removeTaskFromQueue(@NonNull AppSearchRateLimitConfig rateLimitConfig,
            @NonNull String packageName, @CallStats.CallType int apiType) {
        Objects.requireNonNull(rateLimitConfig);
        Objects.requireNonNull(packageName);
        if (!mPerPackageTaskCosts.containsKey(packageName)) {
            Log.e(TAG, "There are no tasks to remove from the queue for package: " + packageName);
            return;
        }
        int apiCost = rateLimitConfig.getApiCost(apiType);
        --mTaskQueueSize;
        mTaskQueueTotalCost -= apiCost;
        removePackageTaskInfo(packageName, apiType, apiCost);
    }

    @NonNull
    public ExecutorService getExecutorService() {
        return mExecutorService;
    }

    public int getTaskQueueSize() {
        return mTaskQueueSize;
    }

    @NonNull
    @VisibleForTesting
    public ArrayMap<String, TaskCostInfo> getPerPackageTaskCosts() {
        return new ArrayMap<>(mPerPackageTaskCosts);
    }

    private void addPackageTaskInfo(@NonNull String packageName, @CallStats.CallType int apiType,
            int apiCost) {
        TaskCostInfo packageTaskCostInfo = mPerPackageTaskCosts.get(packageName);
        if (packageTaskCostInfo == null) {
            packageTaskCostInfo = new TaskCostInfo(0, 0);
            mPerPackageTaskCosts.put(packageName, packageTaskCostInfo);
        }
        ++packageTaskCostInfo.mTaskCount;
        packageTaskCostInfo.mTotalTaskCost += apiCost;
        packageTaskCostInfo.incrementApiTaskCount(apiType);
    }

    private void removePackageTaskInfo(@NonNull String packageName, @CallStats.CallType int apiType,
            int apiCost) {
        TaskCostInfo packageTaskCostInfo = mPerPackageTaskCosts.get(packageName);
        if (packageTaskCostInfo == null) {
            Log.e(TAG, "There are no tasks to remove from the queue for package: " + packageName);
            return;
        }
        --packageTaskCostInfo.mTaskCount;
        packageTaskCostInfo.mTotalTaskCost -= apiCost;
        packageTaskCostInfo.decrementApiTaskCount(apiType);
        if (packageTaskCostInfo.mTaskCount <= 0 || packageTaskCostInfo.mTotalTaskCost <= 0) {
            mPerPackageTaskCosts.remove(packageName);
        }
    }

    /**
     * Class containing the integer pair of task count and total task costs.
     */
    public static final class TaskCostInfo {
        public int mTaskCount;
        public int mTotalTaskCost;
        /**
         * A map of {@link CallStats.CallType} -> api task count currently on task queue.
         */
        private final ArrayMap<Integer, Integer> mPerApiTaskCounts = new ArrayMap<>();

        TaskCostInfo(int taskCount, int totalTaskCost) {
            mTaskCount = taskCount;
            mTotalTaskCost = totalTaskCost;
        }

        public int getApiTaskCount(@CallStats.CallType int apiType) {
            return mPerApiTaskCounts.getOrDefault(apiType, 0);
        }

        private void incrementApiTaskCount(@CallStats.CallType int apiType) {
            mPerApiTaskCounts.put(apiType, mPerApiTaskCounts.getOrDefault(apiType, 0) + 1);
        }

        private void decrementApiTaskCount(@CallStats.CallType int apiType) {
            int taskCount = mPerApiTaskCounts.getOrDefault(apiType, 0);
            --taskCount;
            if (taskCount <= 0) {
                mPerApiTaskCounts.remove(apiType);
            } else {
                mPerApiTaskCounts.put(apiType, taskCount);
            }
        }
    }
}
