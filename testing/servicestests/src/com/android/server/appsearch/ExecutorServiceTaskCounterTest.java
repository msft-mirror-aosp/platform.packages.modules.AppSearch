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

package com.android.server.appsearch;

import static com.google.common.truth.Truth.assertThat;

import com.android.server.appsearch.external.localstorage.stats.CallStats;
import com.android.server.appsearch.util.ExecutorManager;
import com.android.server.appsearch.util.ExecutorServiceTaskCounter;

import org.junit.Test;

public class ExecutorServiceTaskCounterTest {
    @Test
    public void testAddTaskToQueue_addOk() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName = "pkgName";

        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(5);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(40);
        assertThat(rateLimitConfig.getApiCost(CallStats.CALL_TYPE_SET_SCHEMA)).isEqualTo(99);

        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).mTaskCount)
                .isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).mTotalTaskCost)
                .isEqualTo(90);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(2);
    }

    @Test
    public void testAddTaskToQueue_exceedsPerPackageCapacity_cannotAddTask() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName = "pkgName";

        // Cannot add task with cost that exceeds the per-package capacity.
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_SET_SCHEMA)).isFalse();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(0);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName))
                .isNull();

        // Add some tasks to fill up the task queue
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).mTotalTaskCost)
                .isEqualTo(90);

        // Can no longer add tasks once per-package capacity is full.
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName,
                CallStats.CALL_TYPE_SEARCH)).isFalse();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).mTaskCount)
                .isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).mTotalTaskCost)
                .isEqualTo(90);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName).getApiTaskCount(
                CallStats.CALL_TYPE_SEARCH)).isEqualTo(0);
    }

    @Test
    public void testAddTaskToQueue_otherPackageExceedsPerPackageCapacity_canAddTask() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName1 = "pkgName1";
        String pkgName2 = "pkgName2";

        // Add some tasks to fill up pkgName1 capacity
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(90);

        // Adding task to pkgName2 is ok
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_SEARCH)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTaskCount)
                .isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(90);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTaskCount)
                .isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_SEARCH)).isEqualTo(0);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testAddTaskToQueue_exceedsTotalCapacity_cannotAddTask() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName1 = "pkgName1";
        String pkgName2 = "pkgName2";

        // Add some tasks to fill up task queue capacity
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_SEARCH)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(90);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(6);

        // Can no longer add to either package once API costs exceeds total capacity
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isFalse();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isFalse();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTaskCount)
                .isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(90);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTaskCount)
                .isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_SEARCH)).isEqualTo(0);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_SEARCH)).isEqualTo(1);
    }

    @Test
    public void testRemoveTaskFromQueue_removeOk() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName1 = "pkgName1";
        String pkgName2 = "pkgName2";

        // Add some tasks to fill up the task queue
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(50);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(50);

        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS);
        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS);
        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_GET_DOCUMENTS);
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(3);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTaskCount)
                .isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(5);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTaskCount)
                .isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(10);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(0);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(0);
    }

    @Test
    public void testCanAddMoreTasksAfterQueueClearsUp() {
        ExecutorServiceTaskCounter executorTaskCounter = new ExecutorServiceTaskCounter(
                ExecutorManager.createDefaultExecutorService());
        AppSearchRateLimitConfig rateLimitConfig = AppSearchRateLimitConfig.create(
                100, 0.9f,
                "localPutDocuments:5;localGetDocuments:40;localSetSchema:99");
        String pkgName1 = "pkgName1";
        String pkgName2 = "pkgName2";

        // Add some tasks to fill up the task queue
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(6);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(50);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(50);

        // Remove some tasks to clear up the queue
        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_PUT_DOCUMENTS);
        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName1,
                CallStats.CALL_TYPE_GET_DOCUMENTS);
        executorTaskCounter.removeTaskFromQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_GET_DOCUMENTS);
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(3);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(5);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(10);

        // Can add more tasks now that queue has cleared up a little
        assertThat(executorTaskCounter.addTaskToQueue(rateLimitConfig, pkgName2,
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isTrue();
        assertThat(executorTaskCounter.getTaskQueueSize()).isEqualTo(4);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTaskCount)
                .isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).mTotalTaskCost)
                .isEqualTo(5);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTaskCount)
                .isEqualTo(3);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).mTotalTaskCost)
                .isEqualTo(50);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(1);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName1).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(0);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_PUT_DOCUMENTS)).isEqualTo(2);
        assertThat(executorTaskCounter.getPerPackageTaskCosts().get(pkgName2).getApiTaskCount(
                CallStats.CALL_TYPE_GET_DOCUMENTS)).isEqualTo(1);
    }
}