/*
 * Copyright (c) 2023-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins.ffi.wrappers;

public interface PostPrimitiveCleanup {
    void cleanup();
}
