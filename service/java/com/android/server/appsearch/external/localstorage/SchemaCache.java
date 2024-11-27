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

package com.android.server.appsearch.external.localstorage;

import android.annotation.NonNull;
import android.app.appsearch.exceptions.AppSearchException;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.appsearch.flags.Flags;
import com.android.server.appsearch.external.localstorage.util.PrefixUtil;

import com.google.android.icing.proto.SchemaTypeConfigProto;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Caches and manages schema information for AppSearch.
 *
 * @hide
 */
public class SchemaCache {
    /**
     * A map that contains schema types and SchemaTypeConfigProtos for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective SchemaTypeConfigProto.
     */
    private final Map<String, Map<String, SchemaTypeConfigProto>> mSchemaMap = new ArrayMap<>();

    /**
     * A map that contains schema types and all children schema types for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective list of children prefixed schema types.
     */
    private final Map<String, Map<String, List<String>>> mSchemaParentToChildrenMap =
            new ArrayMap<>();

    /**
     * A map that contains schema types and all parent schema types for all package-database
     * prefixes. It maps each package-database prefix to an inner-map. The inner-map maps each
     * prefixed schema type to its respective list of unprefixed parent schema types including
     * transitive parents. It's guaranteed that child types always appear before parent types in the
     * list.
     */
    private final Map<String, Map<String, List<String>>>
            mSchemaChildToTransitiveUnprefixedParentsMap = new ArrayMap<>();

    public SchemaCache() {}

    public SchemaCache(@NonNull Map<String, Map<String, SchemaTypeConfigProto>> schemaMap)
            throws AppSearchException {
        mSchemaMap.putAll(Objects.requireNonNull(schemaMap));
        rebuildCache();
    }

    /** Returns the schema map for the given prefix. */
    @NonNull
    public Map<String, SchemaTypeConfigProto> getSchemaMapForPrefix(@NonNull String prefix) {
        Objects.requireNonNull(prefix);

        Map<String, SchemaTypeConfigProto> schemaMap = mSchemaMap.get(prefix);
        if (schemaMap == null) {
            return Collections.emptyMap();
        }
        return schemaMap;
    }

    /** Returns a set of all prefixes stored in the cache. */
    @NonNull
    public Set<String> getAllPrefixes() {
        return Collections.unmodifiableSet(mSchemaMap.keySet());
    }

    /**
     * Returns all prefixed schema types stored in the cache.
     *
     * <p>This method is inefficient to call repeatedly.
     */
    @NonNull
    public List<String> getAllPrefixedSchemaTypes() {
        List<String> cachedPrefixedSchemaTypes = new ArrayList<>();
        for (Map<String, SchemaTypeConfigProto> value : mSchemaMap.values()) {
            cachedPrefixedSchemaTypes.addAll(value.keySet());
        }
        return cachedPrefixedSchemaTypes;
    }

    /**
     * Returns the schema types for the given set of prefixed schema types with their descendants,
     * based on the schema parent-to-children map held in the cache.
     */
    @NonNull
    public Set<String> getSchemaTypesWithDescendants(
            @NonNull String prefix, @NonNull Set<String> prefixedSchemaTypes) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(prefixedSchemaTypes);
        Map<String, List<String>> parentToChildrenMap = mSchemaParentToChildrenMap.get(prefix);
        if (parentToChildrenMap == null) {
            parentToChildrenMap = Collections.emptyMap();
        }

        // Perform a BFS search on the inheritance graph started by the set of prefixedSchemaTypes.
        Set<String> visited = new ArraySet<>();
        Queue<String> prefixedSchemaQueue = new ArrayDeque<>(prefixedSchemaTypes);
        while (!prefixedSchemaQueue.isEmpty()) {
            String currentPrefixedSchema = prefixedSchemaQueue.poll();
            if (visited.contains(currentPrefixedSchema)) {
                continue;
            }
            visited.add(currentPrefixedSchema);
            List<String> children = parentToChildrenMap.get(currentPrefixedSchema);
            if (children == null) {
                continue;
            }
            prefixedSchemaQueue.addAll(children);
        }

        return visited;
    }

    /**
     * Returns the unprefixed parent schema types, including transitive parents, for the given
     * prefixed schema type, based on the schema child-to-parents map held in the cache. It's
     * guaranteed that child types always appear before parent types in the list.
     */
    @NonNull
    public List<String> getTransitiveUnprefixedParentSchemaTypes(
            @NonNull String prefix, @NonNull String prefixedSchemaType) throws AppSearchException {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(prefixedSchemaType);

        // If the flag is on, retrieve the parent types from the cache as it is available.
        // Otherwise, recalculate the parent types.
        if (Flags.enableSearchResultParentTypes()) {
            Map<String, List<String>> unprefixedChildToParentsMap =
                    mSchemaChildToTransitiveUnprefixedParentsMap.get(prefix);
            if (unprefixedChildToParentsMap == null) {
                return Collections.emptyList();
            }
            List<String> parents = unprefixedChildToParentsMap.get(prefixedSchemaType);
            return parents == null ? Collections.emptyList() : parents;
        } else {
            return calculateTransitiveUnprefixedParentSchemaTypes(
                    prefixedSchemaType, getSchemaMapForPrefix(prefix));
        }
    }

    /**
     * Rebuilds the schema parent-to-children and child-to-parents maps for the given prefix, based
     * on the current schema map.
     *
     * <p>The schema parent-to-children and child-to-parents maps must be updated when {@link
     * #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the results from
     * {@link #getSchemaTypesWithDescendants} and {@link #getTransitiveUnprefixedParentSchemaTypes}
     * would be stale.
     */
    public void rebuildCacheForPrefix(@NonNull String prefix) throws AppSearchException {
        Objects.requireNonNull(prefix);

        mSchemaParentToChildrenMap.remove(prefix);
        mSchemaChildToTransitiveUnprefixedParentsMap.remove(prefix);
        Map<String, SchemaTypeConfigProto> prefixedSchemaMap = mSchemaMap.get(prefix);
        if (prefixedSchemaMap == null) {
            return;
        }

        // Build the parent-to-children map for the current prefix.
        Map<String, List<String>> parentToChildrenMap = new ArrayMap<>();
        for (SchemaTypeConfigProto childSchemaConfig : prefixedSchemaMap.values()) {
            for (int i = 0; i < childSchemaConfig.getParentTypesCount(); i++) {
                String parent = childSchemaConfig.getParentTypes(i);
                List<String> children = parentToChildrenMap.get(parent);
                if (children == null) {
                    children = new ArrayList<>();
                    parentToChildrenMap.put(parent, children);
                }
                children.add(childSchemaConfig.getSchemaType());
            }
        }
        // Record the map for the current prefix.
        if (!parentToChildrenMap.isEmpty()) {
            mSchemaParentToChildrenMap.put(prefix, parentToChildrenMap);
        }

        // If the flag is on, build the child-to-parent maps as caches. Otherwise, this
        // information will have to be recalculated when needed.
        if (Flags.enableSearchResultParentTypes()) {
            // Build the child-to-parents maps for the current prefix.
            Map<String, List<String>> childToTransitiveUnprefixedParentsMap = new ArrayMap<>();
            for (SchemaTypeConfigProto childSchemaConfig : prefixedSchemaMap.values()) {
                if (childSchemaConfig.getParentTypesCount() > 0) {
                    childToTransitiveUnprefixedParentsMap.put(
                            childSchemaConfig.getSchemaType(),
                            calculateTransitiveUnprefixedParentSchemaTypes(
                                    childSchemaConfig.getSchemaType(), prefixedSchemaMap));
                }
            }
            // Record the map for the current prefix.
            if (!childToTransitiveUnprefixedParentsMap.isEmpty()) {
                mSchemaChildToTransitiveUnprefixedParentsMap.put(
                        prefix, childToTransitiveUnprefixedParentsMap);
            }
        }
    }

    /**
     * Rebuilds the schema parent-to-children and child-to-parents maps based on the current schema
     * map.
     *
     * <p>The schema parent-to-children and child-to-parents maps must be updated when {@link
     * #addToSchemaMap} or {@link #removeFromSchemaMap} has been called. Otherwise, the results from
     * {@link #getSchemaTypesWithDescendants} and {@link #getTransitiveUnprefixedParentSchemaTypes}
     * would be stale.
     */
    public void rebuildCache() throws AppSearchException {
        mSchemaParentToChildrenMap.clear();
        mSchemaChildToTransitiveUnprefixedParentsMap.clear();
        for (String prefix : mSchemaMap.keySet()) {
            rebuildCacheForPrefix(prefix);
        }
    }

    /**
     * Adds a schema to the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children and child-to-parents
     * maps in the cache, and either {@link #rebuildCache} or {@link #rebuildCacheForPrefix} is
     * required to be called to update the cache.
     */
    public void addToSchemaMap(
            @NonNull String prefix, @NonNull SchemaTypeConfigProto schemaTypeConfigProto) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(schemaTypeConfigProto);

        Map<String, SchemaTypeConfigProto> schemaTypeMap = mSchemaMap.get(prefix);
        if (schemaTypeMap == null) {
            schemaTypeMap = new ArrayMap<>();
            mSchemaMap.put(prefix, schemaTypeMap);
        }
        schemaTypeMap.put(schemaTypeConfigProto.getSchemaType(), schemaTypeConfigProto);
    }

    /**
     * Removes a schema from the schema map.
     *
     * <p>Note that this method will invalidate the schema parent-to-children and child-to-parents
     * maps in the cache, and either {@link #rebuildCache} or {@link #rebuildCacheForPrefix} is
     * required to be called to update the cache.
     */
    public void removeFromSchemaMap(@NonNull String prefix, @NonNull String schemaType) {
        Objects.requireNonNull(prefix);
        Objects.requireNonNull(schemaType);

        Map<String, SchemaTypeConfigProto> schemaTypeMap = mSchemaMap.get(prefix);
        if (schemaTypeMap != null) {
            schemaTypeMap.remove(schemaType);
        }
    }

    /**
     * Removes the entry of the given prefix from the schema map, the schema parent-to-children map
     * and the child-to-parents map, and returns the set of removed prefixed schema type.
     */
    @NonNull
    public Set<String> removePrefix(@NonNull String prefix) {
        Objects.requireNonNull(prefix);

        Map<String, SchemaTypeConfigProto> removedSchemas =
                Objects.requireNonNull(mSchemaMap.remove(prefix));
        mSchemaParentToChildrenMap.remove(prefix);
        mSchemaChildToTransitiveUnprefixedParentsMap.remove(prefix);
        return removedSchemas.keySet();
    }

    /** Clears all data in the cache. */
    public void clear() {
        mSchemaMap.clear();
        mSchemaParentToChildrenMap.clear();
        mSchemaChildToTransitiveUnprefixedParentsMap.clear();
    }

    /**
     * Get the list of unprefixed transitive parent type names of {@code prefixedSchemaType}.
     *
     * <p>It's guaranteed that child types always appear before parent types in the list.
     */
    @NonNull
    private List<String> calculateTransitiveUnprefixedParentSchemaTypes(
            @NonNull String prefixedSchemaType,
            @NonNull Map<String, SchemaTypeConfigProto> prefixedSchemaMap)
            throws AppSearchException {
        // Please note that neither DFS nor BFS order is guaranteed to always put child types
        // before parent types (due to the diamond problem), so a topological sorting algorithm
        // is required.
        Map<String, Integer> inDegreeMap = new ArrayMap<>();
        collectParentTypeInDegrees(
                prefixedSchemaType,
                prefixedSchemaMap,
                /* visited= */ new ArraySet<>(),
                inDegreeMap);

        List<String> result = new ArrayList<>();
        Queue<String> queue = new ArrayDeque<>();
        // prefixedSchemaType is the only type that has zero in-degree at this point.
        queue.add(prefixedSchemaType);
        while (!queue.isEmpty()) {
            SchemaTypeConfigProto currentSchema =
                    Objects.requireNonNull(prefixedSchemaMap.get(queue.poll()));
            for (int i = 0; i < currentSchema.getParentTypesCount(); ++i) {
                String prefixedParentType = currentSchema.getParentTypes(i);
                int parentInDegree =
                        Objects.requireNonNull(inDegreeMap.get(prefixedParentType)) - 1;
                inDegreeMap.put(prefixedParentType, parentInDegree);
                if (parentInDegree == 0) {
                    result.add(PrefixUtil.removePrefix(prefixedParentType));
                    queue.add(prefixedParentType);
                }
            }
        }
        return result;
    }

    private void collectParentTypeInDegrees(
            @NonNull String prefixedSchemaType,
            @NonNull Map<String, SchemaTypeConfigProto> schemaTypeMap,
            @NonNull Set<String> visited,
            @NonNull Map<String, Integer> inDegreeMap) {
        if (visited.contains(prefixedSchemaType)) {
            return;
        }
        visited.add(prefixedSchemaType);
        SchemaTypeConfigProto schema =
                Objects.requireNonNull(schemaTypeMap.get(prefixedSchemaType));
        for (int i = 0; i < schema.getParentTypesCount(); ++i) {
            String prefixedParentType = schema.getParentTypes(i);
            Integer parentInDegree = inDegreeMap.get(prefixedParentType);
            if (parentInDegree == null) {
                parentInDegree = 0;
            }
            inDegreeMap.put(prefixedParentType, parentInDegree + 1);
            collectParentTypeInDegrees(prefixedParentType, schemaTypeMap, visited, inDegreeMap);
        }
    }
}
