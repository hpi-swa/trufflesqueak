/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.util.logging.Level;

import com.oracle.truffle.api.TruffleLogger;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

/**
 * Logging infrastructure for TruffleSqueak. All loggers are consistently defined here, so that it
 * is clear which loggers are available.
 */
public final class LogUtils {

    public static final TruffleLogger MAIN = TruffleLogger.getLogger(SqueakLanguageConfig.ID);

    public static final TruffleLogger ARRAY_STATEGIES = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "array-strategies");
    public static final TruffleLogger DEBUG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "debug");
    public static final TruffleLogger GC = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "gc");
    public static final TruffleLogger IMAGE = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "image");
    public static final TruffleLogger INTEROP = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "interop");
    public static final TruffleLogger INTERPRETER_PROXY = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "interpreter-proxy");
    public static final TruffleLogger INTERRUPTS = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "interrupts");
    public static final TruffleLogger IO = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "io");
    public static final TruffleLogger ITERATE_FRAMES = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "iterate-frames");
    public static final TruffleLogger OBJECT_GRAPH = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "object-graph");
    public static final TruffleLogger PRIMITIVES = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "primitives");
    public static final TruffleLogger READER = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "reader");
    public static final TruffleLogger SCHEDULING = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "scheduling");
    public static final TruffleLogger SOCKET = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "socket");
    public static final TruffleLogger SSL = TruffleLogger.getLogger(SqueakLanguageConfig.ID, "ssl");

    public static final boolean GC_IS_LOGGABLE_FINE = GC.isLoggable(Level.FINE);
}
