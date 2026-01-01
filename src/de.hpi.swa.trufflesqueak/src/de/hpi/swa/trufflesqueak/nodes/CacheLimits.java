/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

public final class CacheLimits {
    public static final int EXECUTE_METHOD_CACHE_LIMIT = 4;
    public static final int INDIRECT_PRIMITIVE_CACHE_LIMIT = 2;
    public static final int INLINE_BLOCK_CACHE_LIMIT = 4;
    public static final int INLINE_METHOD_CACHE_LIMIT = 4;
    public static final int NEW_CACHE_LIMIT = 3;
    public static final int PERFORM_SELECTOR_CACHE_LIMIT = 4;
    public static final int POINTERS_LAYOUT_CACHE_LIMIT = 4;
    public static final int POINTERS_VARIABLE_PART_CACHE_LIMIT = 3;
}
