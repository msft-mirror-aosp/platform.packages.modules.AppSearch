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
package android.app.appsearch.safeparcel;

import android.os.Parcel;

@SafeParcelable.Class(creator = "TestSafeParcelableWithoutRemovedParamCreator")
public class TestSafeParcelableWithoutRemovedParam extends AbstractSafeParcelable {

    public static final Creator<TestSafeParcelableWithoutRemovedParam> CREATOR =
            new TestSafeParcelableWithoutRemovedParamCreator();

    @Constructor
    public TestSafeParcelableWithoutRemovedParam(@Param(id = 1) String oldVal) {
        oldField = oldVal;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        TestSafeParcelableWithoutRemovedParamCreator.writeToParcel(this, out, flags);
    }

    @Field(id = 1, defaultValue = "1")
    public final String oldField;
}