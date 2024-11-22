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

package android.app.appsearch.ast.query;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.app.appsearch.PropertyPath;
import android.app.appsearch.ast.FunctionNode;

import com.android.appsearch.flags.Flags;

import java.util.Objects;

/**
 * {@link FunctionNode} representing the `hasProperty` query function.
 *
 * <p>The `hasProperty` query function will return all documents that contain the given property and
 * have values in the given property.
 */
@FlaggedApi(Flags.FLAG_ENABLE_ABSTRACT_SYNTAX_TREES)
public final class HasPropertyNode implements FunctionNode {
    private PropertyPath mProperty;

    /**
     * Constructor for a {@link HasPropertyNode} representing the query function `hasProperty`.
     *
     * @param property A {@link PropertyPath} representing the property to check whether or not it
     *     contains a value in the document.
     */
    public HasPropertyNode(@NonNull PropertyPath property) {
        mProperty = Objects.requireNonNull(property);
    }

    /**
     * Returns the name of the function represented by {@link HasPropertyNode}, stored in the enum
     * {@link FunctionNode#FUNCTION_NAME_HAS_PROPERTY}.
     */
    @NonNull
    @Override
    @FunctionName
    public String getFunctionName() {
        return FunctionNode.FUNCTION_NAME_HAS_PROPERTY;
    }

    /**
     * Gets the {@link PropertyPath} representing the property being checked for some value in the
     * document.
     */
    @NonNull
    public PropertyPath getProperty() {
        return mProperty;
    }

    /**
     * Sets the {@link PropertyPath} representing the property being checked for some value in the
     * document.
     */
    public void setProperty(@NonNull PropertyPath property) {
        mProperty = Objects.requireNonNull(property);
    }

    /**
     * Get the string representation of {@link HasPropertyNode}.
     *
     * <p>The string representation of {@link HasPropertyNode} is the function name followed by the
     * property path in quotes.
     */
    @NonNull
    @Override
    public String toString() {
        return FunctionNode.FUNCTION_NAME_HAS_PROPERTY + "(\"" + mProperty + "\")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HasPropertyNode that = (HasPropertyNode) o;
        return Objects.equals(mProperty, that.mProperty);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mProperty);
    }
}
