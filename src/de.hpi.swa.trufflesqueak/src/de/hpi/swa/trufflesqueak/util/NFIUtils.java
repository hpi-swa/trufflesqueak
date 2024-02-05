/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.source.Source;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;

import java.io.File;

public final class NFIUtils {

    @ExportLibrary(InteropLibrary.class)
    public static class TruffleExecutable implements TruffleObject {
        public final String nfiSignature;
        final ITruffleExecutable executable;

        public TruffleExecutable(final String nfiSignature, final ITruffleExecutable executable) {
            this.nfiSignature = nfiSignature;
            this.executable = executable;
        }

        public static <R> TruffleExecutable wrap(final String nfiSignature, final TruffleSupplier<R> supplier) {
            return new TruffleExecutable(nfiSignature, supplier);
        }

        public static <T, R> TruffleExecutable wrap(final String nfiSignature, final TruffleFunction<T, R> function) {
            return new TruffleExecutable(nfiSignature, function);
        }

        public static <S, T, R> TruffleExecutable wrap(final String nfiSignature, final TruffleBiFunction<S, T, R> function) {
            return new TruffleExecutable(nfiSignature, function);
        }

        public static <S, T, U, R> TruffleExecutable wrap(final String nfiSignature, final TruffleTriFunction<S, T, U, R> function) {
            return new TruffleExecutable(nfiSignature, function);
        }

        public static <S, T, U, V, R> TruffleExecutable wrap(final String nfiSignature, final TruffleQuadFunction<S, T, U, V, R> function) {
            return new TruffleExecutable(nfiSignature, function);
        }

        public static <S, T, U, V, W, R> TruffleExecutable wrap(final String nfiSignature, final TruffleQuintFunction<S, T, U, V, W, R> function) {
            return new TruffleExecutable(nfiSignature, function);
        }

        @ExportMessage
        boolean isExecutable() {
            return true;
        }

        @ExportMessage
        @TruffleBoundary
        Object execute(final Object... arguments) {
            return executable.execute(arguments);
        }

        public TruffleClosure createClosure(final SqueakImageContext context) throws UnsupportedTypeException {
            return new TruffleClosure(context, this);
        }
    }

    @ExportLibrary(value = InteropLibrary.class, delegateTo = "closure")
    public static class TruffleClosure implements TruffleObject {
        public final TruffleExecutable executable;

        final Object closure;

        public TruffleClosure(final SqueakImageContext context, final TruffleExecutable executable)
                        throws UnsupportedTypeException {
            this.executable = executable;
            this.closure = createClosure(context, executable, executable.nfiSignature);
        }
    }

    public interface ITruffleExecutable {
        Object execute(Object... arguments);
    }

    @FunctionalInterface
    public interface TruffleSupplier<R> extends ITruffleExecutable {
        R run();

        default Object execute(final Object... arguments) {
            assert arguments.length == 0;
            return run();
        }
    }

    @FunctionalInterface
    public interface TruffleFunction<T, R> extends ITruffleExecutable {
        R run(T argument);

        @SuppressWarnings("unchecked")
        default Object execute(final Object... arguments) {
            assert arguments.length == 1;
            return run((T) arguments[0]);
        }
    }

    @FunctionalInterface
    public interface TruffleBiFunction<S, T, R> extends ITruffleExecutable {
        R run(S argument1, T argument2);

        @SuppressWarnings("unchecked")
        default Object execute(final Object... arguments) {
            assert arguments.length == 2;
            return run((S) arguments[0], (T) arguments[1]);
        }
    }

    @FunctionalInterface
    public interface TruffleTriFunction<S, T, U, R> extends ITruffleExecutable {
        R run(S argument1, T argument2, U argument3);

        @SuppressWarnings("unchecked")
        default Object execute(final Object... arguments) {
            assert arguments.length == 3;
            return run((S) arguments[0], (T) arguments[1], (U) arguments[2]);
        }
    }

    @FunctionalInterface
    public interface TruffleQuadFunction<S, T, U, V, R> extends ITruffleExecutable {
        R run(S argument1, T argument2, U argument3, V argument4);

        @SuppressWarnings("unchecked")
        default Object execute(final Object... arguments) {
            assert arguments.length == 4;
            return run((S) arguments[0], (T) arguments[1], (U) arguments[2], (V) arguments[3]);
        }
    }

    @FunctionalInterface
    public interface TruffleQuintFunction<S, T, U, V, W, R> extends ITruffleExecutable {
        R run(S argument1, T argument2, U argument3, V argument4, W argument5);

        @SuppressWarnings("unchecked")
        default Object execute(final Object... arguments) {
            assert arguments.length == 5;
            return run((S) arguments[0], (T) arguments[1], (U) arguments[2], (V) arguments[3], (W) arguments[4]);
        }
    }

    public static Object executeNFI(final SqueakImageContext context, final String nfiCode) {
        final Source source = Source.newBuilder("nfi", nfiCode, "native").build();
        return context.env.parseInternal(source).call();
    }

    public static Object loadLibrary(final SqueakImageContext context, final String moduleName, final String boundSymbols) {
        final String libName = System.mapLibraryName(moduleName);
        TruffleFile libPath = context.getHomePath().resolve("lib" + File.separatorChar + libName);
        if (!libPath.exists()) {
            // to preserve compatibility with plugins from opensmalltalk, also check without 'lib'
            // prefix
            if (libName.startsWith("lib")) {
                libPath = context.getHomePath().resolve("lib" + File.separatorChar + libName.replace("lib", ""));
                if (!libPath.exists()) {
                    return null;
                }
            } else {
                return null;
            }
        }
        final String nfiCode = "load \"" + libPath.getAbsoluteFile().getPath() + "\" " + boundSymbols;
        return executeNFI(context, nfiCode);
    }

    public static Object createSignature(final SqueakImageContext context, final String signature) {
        return executeNFI(context, signature);
    }

    public static Object invokeSignatureMethod(final SqueakImageContext context, final String signature, final String method, final Object... args)
                    throws UnsupportedMessageException, UnknownIdentifierException, UnsupportedTypeException, ArityException {
        final Object nfiSignature = createSignature(context, signature);
        final InteropLibrary signatureInteropLibrary = getInteropLibrary(nfiSignature);
        return signatureInteropLibrary.invokeMember(nfiSignature, method, args);
    }

    public static Object createClosure(final SqueakImageContext context, final Object executable, final String signature)
                    throws UnsupportedTypeException {
        try {
            return invokeSignatureMethod(context, signature, "createClosure", executable);
        } catch (UnsupportedMessageException | UnknownIdentifierException | ArityException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    public static Object loadMember(final SqueakImageContext context, final Object library, final String name, final String signature)
                    throws UnknownIdentifierException {
        final InteropLibrary interopLibrary = getInteropLibrary(library);
        try {
            final Object symbol = interopLibrary.readMember(library, name);
            return invokeSignatureMethod(context, signature, "bind", symbol);
        } catch (UnsupportedMessageException | UnsupportedTypeException | ArityException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    public static InteropLibrary getInteropLibrary(final Object loadedLibrary) {
        return InteropLibrary.getFactory().getUncached(loadedLibrary);
    }
}
