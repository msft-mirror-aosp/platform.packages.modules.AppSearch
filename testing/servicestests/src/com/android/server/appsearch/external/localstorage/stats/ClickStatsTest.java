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

package com.android.server.appsearch.external.localstorage.stats;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ClickStatsTest {
    @Test
    public void testBuilder() {
        long timestampMillis = 1L;
        long timeStayOnResultMillis = 2L;
        int resultRankInBlock = 3;
        int resultRankGlobal = 4;
        boolean isGoodClick = false;

        final ClickStats clickStats =
                new ClickStats.Builder()
                        .setTimestampMillis(timestampMillis)
                        .setTimeStayOnResultMillis(timeStayOnResultMillis)
                        .setResultRankInBlock(resultRankInBlock)
                        .setResultRankGlobal(resultRankGlobal)
                        .setIsGoodClick(isGoodClick)
                        .build();

        assertThat(clickStats.getTimestampMillis()).isEqualTo(timestampMillis);
        assertThat(clickStats.getTimeStayOnResultMillis()).isEqualTo(timeStayOnResultMillis);
        assertThat(clickStats.getResultRankInBlock()).isEqualTo(resultRankInBlock);
        assertThat(clickStats.getResultRankGlobal()).isEqualTo(resultRankGlobal);
        assertThat(clickStats.isGoodClick()).isEqualTo(isGoodClick);
    }
}
