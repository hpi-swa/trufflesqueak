/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interpreterproxy;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.util.DebugUtils;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.OS;

@SuppressWarnings("restricted")
public final class InterpreterProxySupport {
    public static TruffleFile findLibrary(final SqueakImageContext context, final String moduleName) {
        final TruffleFile libraryPath = context.getLibraryPath();

        /* Try to resolve the common case first. */
        final String libName = System.mapLibraryName(moduleName);
        final TruffleFile systemLibraryPath = libraryPath.resolve(libName);
        if (systemLibraryPath.exists()) {
            return systemLibraryPath;
        }

        if (OS.isMacOS()) {
            /* Try to resolve macOS bundles. */
            final TruffleFile bundleLibrary = libraryPath.resolve(moduleName + ".bundle/Contents/MacOS/" + moduleName);
            if (bundleLibrary.exists()) {
                return bundleLibrary;
            }
        } else {
            /* Try to resolve without 'lib' prefix for compatibility with OSVM plugins. */
            if (libName.startsWith("lib")) {
                final TruffleFile alternativeLibraryPath = libraryPath.resolve(libName.substring(3));
                if (alternativeLibraryPath.exists()) {
                    return alternativeLibraryPath;
                }
            }
        }

        /* All attempts have failed. */
        return null;
    }

    @SuppressWarnings("restricted")
    public static MethodHandle loadMethodHandle(final String moduleName, final String functionName) {
        CompilerAsserts.neverPartOfCompilation();
        final SymbolLookup moduleLibrary = lookupModuleLibrary(moduleName);
        if (moduleLibrary == null) {
            return null;
        }
        final MemorySegment symbol = moduleLibrary.find(functionName).orElseThrow(() -> new RuntimeException("Function '" + functionName + "' not found in library"));
        final FunctionDescriptor descriptor = FunctionDescriptor.of(ValueLayout.JAVA_LONG);
        return Linker.nativeLinker().downcallHandle(symbol, descriptor);
    }

    @SuppressWarnings("restricted")
    private static SymbolLookup lookupModuleLibrary(final String moduleName) {
        final SqueakImageContext context = SqueakImageContext.getSlow();
        final SymbolLookup moduleLibrary = context.loadedLibraries.computeIfAbsent(moduleName, (String _) -> {
            if (context.loadedLibraries.containsKey(moduleName)) {
                return null; // if moduleName was associated with null
            }

            final TruffleFile libPath = findLibrary(context, moduleName);
            if (libPath == null) {
                return null;
            }

            final SymbolLookup lookup = SymbolLookup.libraryLookup(libPath.getPath(), Arena.global());

            final Optional<MemorySegment> initialiseModuleSymbol = lookup.find("initialiseModule");
            if (initialiseModuleSymbol.isPresent()) {
                final FunctionDescriptor initialiseModuleDescriptor = FunctionDescriptor.of(ValueLayout.JAVA_LONG);
                final MethodHandle initialiseModuleHandle = Linker.nativeLinker().downcallHandle(initialiseModuleSymbol.get(), initialiseModuleDescriptor);
                try {
                    final long result = (long) initialiseModuleHandle.invoke();
                    if (result != 1) {
                        LogUtils.INTERPRETER_PROXY.warning("initialiseModule return unexpected result: " + result);
                    }
                } catch (Throwable e) {
                    LogUtils.INTERPRETER_PROXY.warning(() -> DebugUtils.toString(e));
                    return null;
                }
            } else {
                LogUtils.INTERPRETER_PROXY.fine(() -> "Module '" + moduleName + "' does not have an 'initialiseModule'");
            }

            final Optional<MemorySegment> setInterpreterSymbol = lookup.find("setInterpreter");
            if (setInterpreterSymbol.isPresent()) {
                final FunctionDescriptor setInterpreterSymbolDescriptor = FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS);
                final MethodHandle setInterpreterSymbolHandle = Linker.nativeLinker().downcallHandle(setInterpreterSymbol.get(), setInterpreterSymbolDescriptor);
                try {
                    final long result = (long) setInterpreterSymbolHandle.invoke(InterpreterProxy.SINGLETON.getPointer());
                    if (result != 1) {
                        LogUtils.INTERPRETER_PROXY.warning("setInterpreter return unexpected result: " + result);
                    }
                } catch (Throwable e) {
                    LogUtils.INTERPRETER_PROXY.warning(() -> DebugUtils.toString(e));
                    return null;
                }
            } else {
                LogUtils.INTERPRETER_PROXY.warning(() -> "Could not find 'setInterpreter' for module '" + moduleName + "'");
            }
            return lookup;
        });

        // computeIfAbsent would not put null value
        context.loadedLibraries.putIfAbsent(moduleName, moduleLibrary);
        return moduleLibrary;
    }
}
