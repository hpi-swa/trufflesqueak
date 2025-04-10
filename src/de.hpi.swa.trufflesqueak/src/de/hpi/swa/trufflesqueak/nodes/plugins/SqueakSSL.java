/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedBranchProfile;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

/**
 * Implement Squeak SSL primitives using {@link SSLEngine}.
 *
 * <p>
 * <h3>Debugging</h3> <br/>
 * SSL engine debug output can be obtained via system properties, e.g.:
 *
 * <pre>
 * <code>
 *   -Djavax.net.debug=ssl:handshake
 * </code>
 * </pre>
 *
 * to obtain (very) detailed information on the SSL handshake on standard out.
 * </p>
 *
 * <p>
 * <h3>Numeric Return Code Convention</h3> <br/>
 * <table>
 * <tr>
 * <td>&gt; 0</td>
 * <td>Number of bytes to be sent to the server / client</td>
 * </tr>
 * <td>0</td>
 * <td>Success. The connection is established.</td>
 * </tr>
 * <tr>
 * <td>-1</td>
 * <td>More input is required.</td>
 * </tr>
 * <tr>
 * <td>&lt; -1</td>
 * <td>Other errors.</td>
 * </tr>
 * </table>
 * </p>
 *
 * <p>
 * To-Do
 *
 * <ul>
 * <li>Retest connection close events - is it possible to re-enter UNUSED state and begin from
 * scratch?</li>
 * <li>Document TLS 1.3 compat - what would be required?</li>
 * <li>Implement getter for certificate state</li>
 * <li>Implement getter for certificate name</li>
 * <li>Use log level property to guard prints to STDOUT - maybe add a few more meaningful events?
 * </li>
 * <li>Document / fix why certificate name is a global state - it is only set once, and then re-used
 * across several SSL instances (testConnectAccept assigns the cert name to the client instance
 * once, and expects the server to accept it).</li>
 * </ul>
 * </p>
 *
 * <p>
 * Note for running Squeak tests on Mac OS: if problems crop up, these are the selectors containing
 * platform checks. Especially the error message "no cipher suites in common" likely points to a
 * selector which excludes OS X from file-out of the required custom certificate.
 *
 * <pre>
 * SqueakSSL class ensureSampleCert
 * SqueakSSL class ensureSampleCertFile
 * SqueakSSLTest expectedFailures
 * </pre>
 *
 * You may wish to remove platform checks for OS X and ensure that, similar to the approach for
 * Linux, indeed the custom certificate is written to disk in the test setup.
 * </p>
 *
 */
public final class SqueakSSL extends AbstractPrimitiveFactoryHolder {
    /*
     * On JDK 11, TLS 1.3 would be selected. However, this does not seem to be operational. After
     * exchanging ClientHello and ServerHello, the client instance writes 6 bytes, the
     * "Client Change Cipher Spec", which the server can successfully read. Then both engines end up
     * in "NEED_UNWRAP" state (expecting to be fed new input), however, both engines refuse to
     * produce new data. TLS 1.3 details: https://tls13.ulfheim.net/
     */
    private static final String[] ENABLED_PROTOCOLS = {"TLSv1.2"};

    private static final ByteBuffer EMPTY_BUFFER = createEmptyImmutableBuffer();

    // FIXME global state
    private static String certificateName;

    private enum ReturnCode implements HasId {
        OK(0),
        NEED_MORE_DATA(-1),
        INVALID_STATE(-2),
        BUFFER_TOO_SMALL(-3),
        INPUT_TOO_LARGE(-4),
        GENERIC_ERROR(-5),
        OUT_OF_MEMORY(-6);

        private final long value;

        ReturnCode(final long value) {
            this.value = value;
        }

        @Override
        public long id() {
            return value;
        }
    }

    private enum State implements HasId {
        UNUSED(0),
        ACCEPTING(1),
        CONNECTING(2),
        CONNECTED(3);

        private final long value;

        State(final long value) {
            this.value = value;
        }

        @Override
        public long id() {
            return value;
        }
    }

    private enum IntProperty implements HasId {
        VERSION(0),
        LOG_LEVEL(1),
        SSL_STATE(2),
        CERTIFICATE_STATE(3);

        private final long value;

        IntProperty(final long value) {
            this.value = value;
        }

        @Override
        public long id() {
            return value;
        }
    }

    private enum StringProperty implements HasId {
        PEER_NAME(0),
        CERTIFICATE_NAME(1),
        SERVER_NAME(2);

        private final long value;

        StringProperty(final long value) {
            this.value = value;
        }

        @Override
        public long id() {
            return value;
        }
    }

    /**
     * Annotate an Enumeration with an ID. The ordinal number is insufficient, because we also need
     * to represent negative numbers, and the assigned value should be resistant to changes in
     * declaration order.
     */
    private interface HasId {
        long id();
    }

    @TruffleBoundary
    private static <T extends Enum<T> & HasId> T propertyWithId(final Class<T> enumCls, final long id) {
        for (final T t : enumCls.getEnumConstants()) {
            if (t.id() == id) {
                return t;
            }
        }
        return null;
    }

    private static final class Constants {
        private static final long VERSION = 3;
    }

    private static ByteBuffer createEmptyImmutableBuffer() {
        return ByteBuffer.allocate(0).asReadOnlyBuffer();
    }

    private static class SqSSL {
        private State state = State.UNUSED;
        private SSLContext context;
        private SSLEngine engine;

        /* Hack: Use "*" to avoid certificate validation errors and NPE. */
        private String peerName = "*";
        private String serverName;

        private ByteBuffer buffer;

        @SuppressWarnings("unused" /* TODO */) private long logLevel;
    }

    private static SqSSL getSSLOrNull(final PointersObject handle) {
        final Object sqSSL = handle.getHiddenObject();
        if (sqSSL instanceof final SqSSL o) {
            return o;
        } else {
            return null;
        }
    }

    private static int getBufferSize(final SqSSL ssl) {
        final SSLSession session = ssl.engine.getSession();
        return ssl.engine.getUseClientMode() ? session.getApplicationBufferSize() : session.getPacketBufferSize();
    }

    @FunctionalInterface
    private interface Encoder {
        SSLEngineResult encode(ByteBuffer in, ByteBuffer out) throws SSLException;
    }

    private static SSLEngineResult wrap(final SqSSL ssl, final ByteBuffer sourceBuffer, final ByteBuffer targetBuffer) throws SSLException {
        return encode(ssl, ssl.engine::wrap, sourceBuffer, targetBuffer);
    }

    /**
     * Unwrap a single SSL packet. If no output is expected, the target buffer is allowed to be
     * read-only.
     */
    private static SSLEngineResult unwrap(final SqSSL ssl, final ByteBuffer sourceBuffer, final ByteBuffer targetBuffer) throws SSLException {
        return encode(ssl, ssl.engine::unwrap, sourceBuffer, targetBuffer);
    }

    @TruffleBoundary
    private static SSLEngineResult encode(final SqSSL ssl, final Encoder encoder, final ByteBuffer sourceBuffer, final ByteBuffer targetBuffer) throws SSLException {
        final int bufferSize = getBufferSize(ssl);
        ByteBuffer intermediateTarget = ByteBuffer.allocate(bufferSize);

        while (true) {
            final SSLEngineResult result = encoder.encode(sourceBuffer, intermediateTarget);
            switch (result.getStatus()) {
                case BUFFER_UNDERFLOW -> {
                    return result;
                }
                case BUFFER_OVERFLOW -> {
                    intermediateTarget = enlargeBufferFrom(intermediateTarget, bufferSize);
                    continue;
                }
                case OK -> {
                    intermediateTarget.flip();
                    if (intermediateTarget.remaining() > 0) {
                        targetBuffer.put(intermediateTarget);
                    }
                    return result;
                }
                case CLOSED -> {
                    intermediateTarget.flip();
                    targetBuffer.put(intermediateTarget);
                    return result;
                }
                default -> throw SqueakException.create("Unknown SSL engine status");
            }
        }
    }

    private static ByteBuffer enlargeBufferFrom(final ByteBuffer buffer, final int bufferSize) {
        final int delta = buffer.position() > 0 ? buffer.position() : bufferSize;
        final ByteBuffer newBuffer = ByteBuffer.allocate(bufferSize + delta);
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    /**
     * @param buffer the Squeak buffer object (byte type)
     * @param start the <b>one-based</b> start index
     * @param length the length to interpret
     * @return a read-only byte buffer
     */
    @TruffleBoundary
    private static ByteBuffer asReadBuffer(final NativeObject buffer, final long start, final long length) {
        return ByteBuffer.wrap(buffer.getByteStorage(), (int) start - 1, (int) length).asReadOnlyBuffer();
    }

    /**
     * @param buffer the Squeak buffer object (byte type)
     * @return a write-through byte buffer
     */
    @TruffleBoundary
    private static ByteBuffer asWriteBuffer(final NativeObject buffer) {
        return ByteBuffer.wrap(buffer.getByteStorage());
    }

    /**
     * Assert that the current engine handshake state is contained in a set of expected states,
     * which the caller can handle meaningfully.
     *
     * <p>
     * At the same time, defend against a common programming mistake: according to the
     * documentation, the engine never returns {@link HandshakeStatus#FINISHED}. The finished state
     * is only ever part of {@link SSLEngineResult}.
     * </p>
     */
    @TruffleBoundary
    private static void checkHandshake(final String message, final SqSSL ssl, final HandshakeStatus... expected) {
        assert !ArrayUtils.contains(expected, HandshakeStatus.FINISHED) : "State FINISHED is never returned from engine";
        final HandshakeStatus actual = ssl.engine.getHandshakeStatus();
        if (!ArrayUtils.contains(expected, actual)) {
            throw new IllegalStateException(String.format("Handshake status %s expected, actual: %s. %s",
                            Arrays.toString(expected), actual, message));
        }
    }

    @TruffleBoundary
    private static void checkHandshake(final String message, final SSLEngineResult result, final HandshakeStatus... expected) {
        final HandshakeStatus actual = result.getHandshakeStatus();
        if (!ArrayUtils.contains(expected, actual)) {
            throw new IllegalStateException(String.format("Handshake status %s expected, actual: %s. %s",
                            Arrays.toString(expected), actual, message));
        }
    }

    @TruffleBoundary
    private static void checkStatus(final String message, final SSLEngineResult result, final SSLEngineResult.Status... expected) {
        final SSLEngineResult.Status actual = result.getStatus();
        if (!ArrayUtils.contains(expected, actual)) {
            throw new IllegalStateException(String.format("Status %s expected, actual: %s. %s", Arrays.toString(expected), actual, message));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAccept")
    protected abstract static class PrimAcceptNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        /**
         * Starts or continues a server handshake using the provided data. Will eventually produce
         * output to be sent to the client.
         *
         * <p>
         * In unused state, set up the receiving end (server-side) of a SSL / TLS connection. Then
         * eagerly process incoming client requests (Client Hello), as well as eagerly write the
         * server's responses. Again, similar to the connect primitive, emitting as much SSL packets
         * as possible is an implementation detail fixed by {@code testConnectAccept}, which demands
         * that handshakes occur in exactly four exchanges.
         * </p>
         *
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param sourceBuffer the source buffer; containing client request
         * @param start the one-based start index
         * @param length the length of the data to interpret in the source buffer
         * @param targetBuffer the target buffer; containing server response
         * @return see numeric return type convention from plugin Javadoc
         */

        @Specialization(guards = {"sourceBuffer.isByteType()", "targetBuffer.isByteType()"})
        protected final long doAccept(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSLOrNull(sslHandle);
            if (ssl == null || ssl.state != State.UNUSED && ssl.state != State.ACCEPTING) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer sourceOrNull;
            if (sourceBuffer.getByteLength() > 0) {
                sourceOrNull = asReadBuffer(sourceBuffer, start, length);
            } else {
                sourceOrNull = null;
            }
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                return process(ssl, sourceOrNull, target);
            } catch (final SSLHandshakeException e) {
                return ReturnCode.GENERIC_ERROR.id();
            } catch (final SSLException e) {
                getContext().printToStdErr(e);
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        @TruffleBoundary
        private static long process(final SqSSL ssl, final ByteBuffer sourceOrNull, final ByteBuffer target) throws SSLException {
            if (ssl.state == State.UNUSED) {
                ssl.state = State.ACCEPTING;
                setUp(ssl);
                ssl.engine.setUseClientMode(false);
            }

            assert ssl.state == State.ACCEPTING;
            if (sourceOrNull != null) {
                ssl.buffer.put(sourceOrNull);
            }
            unwrapEagerly(ssl);
            wrapEagerly(ssl, target);
            if (ssl.state == State.CONNECTED) {
                return ReturnCode.OK.id();
            } else {
                final int pos = target.position();
                return pos == 0 ? ReturnCode.NEED_MORE_DATA.id() : pos;
            }
        }

        private static void unwrapEagerly(final SqSSL ssl) throws SSLException {
            do {
                ssl.buffer.flip();
                final SSLEngineResult result = unwrap(ssl, ssl.buffer, EMPTY_BUFFER);
                ssl.buffer.compact();
                checkStatus("Handshake unwrap", result, Status.OK, Status.BUFFER_UNDERFLOW);
                if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
                    break;
                }

                runTasks(ssl);
            } while (ssl.engine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP);
        }

        private static void wrapEagerly(final SqSSL ssl, final ByteBuffer target) throws SSLException {
            HandshakeStatus status = ssl.engine.getHandshakeStatus();
            while (status == HandshakeStatus.NEED_WRAP) {
                final SSLEngineResult result = wrap(ssl, EMPTY_BUFFER, target);
                checkStatus("Handshake wrap", result, Status.OK);
                runTasks(ssl);
                if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                    handshakeCompleted(ssl);
                }
                status = ssl.engine.getHandshakeStatus();
            }
        }

        private static void handshakeCompleted(final SqSSL ssl) {
            ssl.state = State.CONNECTED;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveConnect")
    protected abstract static class PrimConnectNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        /**
         * Starts or continues a client handshake using the provided data. Will eventually produce
         * output to be sent to the server.
         *
         * <p>
         * Specifically, an empty source buffer in combination with an SSL handle in unused state
         * can be passed, and an SSL Client Hello will be sent to the server. Second, a SSL handle
         * in connecting state, along with the server's response (Server Hello), can be passed to
         * complete the handshake.
         * </p>
         *
         * <p>
         * While it is fine for the other methods (e.g., decrypt) to unpack a single SSL packet at a
         * time, connect has to eagerly interpret as many SSL packets from the payload as possible.
         * This implementation detail is fixed by {@code testConnectAccept}, which demands that a
         * handshake has to occur in exactly four data transfers.
         * </p>
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param sourceBuffer the source buffer; may be empty or contain the server's response
         * @param start one-based start index
         * @param length the length of the data to interpret in the source buffer
         * @param targetBuffer the target buffer; contains data to be sent to the server
         * @return see numeric return type convention from plugin Javadoc
         */
        @Specialization(guards = {"sourceBuffer.isByteType()", "targetBuffer.isByteType()"})
        protected final long doConnect(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSLOrNull(sslHandle);
            if (ssl == null || ssl.state != State.UNUSED && ssl.state != State.CONNECTING) {
                return ReturnCode.INVALID_STATE.id();
            }
            final ByteBuffer sourceOrNull;
            if (sourceBuffer.getByteLength() > 0) {
                sourceOrNull = asReadBuffer(sourceBuffer, start, length);
            } else {
                sourceOrNull = null;
            }
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                return processHandshake(ssl, sourceOrNull, target);
            } catch (final SSLException e) {
                getContext().printToStdErr(e);
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        @TruffleBoundary
        private static long processHandshake(final SqSSL ssl, final ByteBuffer sourceOrNull, final ByteBuffer target) throws SSLException {
            /* Establish initial connection */
            if (ssl.state == State.UNUSED) {
                beginHandshake(ssl, target);
            }
            assert ssl.state == State.CONNECTING;
            if (sourceOrNull != null) {
                ssl.buffer.put(sourceOrNull);
            }
            readHandshakeResponse(ssl);
            writeHandshakeResponse(ssl, target);
            if (ssl.state == State.CONNECTED) {
                return ReturnCode.OK.id();
            } else {
                final int pos = target.position();
                return pos == 0 ? ReturnCode.NEED_MORE_DATA.id() : pos;
            }
        }

        private static void beginHandshake(final SqSSL ssl, final ByteBuffer target) throws SSLException {
            ssl.state = State.CONNECTING;
            setUp(ssl);
            ssl.engine.setUseClientMode(true);

            final SSLEngineResult result = wrap(ssl, EMPTY_BUFFER, target);
            checkStatus("Client Hello succeeded", result, Status.OK);
            checkHandshake("Require server response after Client Hello", result, HandshakeStatus.NEED_UNWRAP);
        }

        private static void readHandshakeResponse(final SqSSL ssl) throws SSLException {
            HandshakeStatus status = ssl.engine.getHandshakeStatus();
            while (status == HandshakeStatus.NEED_UNWRAP) {

                ssl.buffer.flip();
                final SSLEngineResult result = unwrap(ssl, ssl.buffer, EMPTY_BUFFER);
                ssl.buffer.compact();

                checkStatus("Processing server handshake response",
                                result, Status.OK, Status.BUFFER_UNDERFLOW);

                if (result.getStatus() == Status.OK) {
                    runTasks(ssl);
                    if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
                        handshakeCompleted(ssl);
                        break;
                    }

                    checkHandshake("Handshake", ssl, HandshakeStatus.NEED_WRAP, HandshakeStatus.NEED_UNWRAP);
                }

                if (result.getStatus() == Status.BUFFER_UNDERFLOW) {
                    break;
                }

                status = ssl.engine.getHandshakeStatus();
            }
        }

        private static void handshakeCompleted(final SqSSL ssl) {
            ssl.state = State.CONNECTED;
            final String peerHost = ssl.engine.getPeerHost();
            if (peerHost != null) {
                ssl.peerName = peerHost;
            }
        }

        private static void writeHandshakeResponse(final SqSSL ssl, final ByteBuffer target) throws SSLException {
            HandshakeStatus status = ssl.engine.getHandshakeStatus();
            while (status == HandshakeStatus.NEED_WRAP) {
                final SSLEngineResult result = wrap(ssl, EMPTY_BUFFER, target);
                checkStatus("Handshake wrap", result, Status.OK, Status.CLOSED);
                runTasks(ssl);
                status = ssl.engine.getHandshakeStatus();
            }
        }
    }

    private static void runTasks(final SqSSL ssl) {
        Runnable task;
        while ((task = ssl.engine.getDelegatedTask()) != null) {
            task.run();
        }
    }

    @TruffleBoundary
    private static void setUp(final SqSSL ssl) {
        if (certificateName != null && !MiscUtils.isBlank(certificateName)) {
            initializeWithCertificate(ssl, certificateName);
        } else {
            initializeWithDefaultCertificates(ssl);
        }
        ensureEngine(ssl);
    }

    private static void initializeWithCertificate(final SqSSL ssl, final String certificate) {
        final Path certificatePath = Paths.get(certificate);
        try {
            ssl.context = SSLContextInitializer.createSSLContext(certificatePath);
        } catch (final IOException e) {
            throw CompilerDirectives.shouldNotReachHere("Failed to load certificate " + certificate + ". Does the file exist?", e);
        } catch (final GeneralSecurityException e) {
            throw CompilerDirectives.shouldNotReachHere("Security error when loading certificate " + certificate, e);
        }
    }

    private static void initializeWithDefaultCertificates(final SqSSL ssl) {
        try {
            ssl.context = SSLContext.getInstance("TLS");
            ssl.context.init(null, null, null);
        } catch (final GeneralSecurityException e) {
            throw CompilerDirectives.shouldNotReachHere("Failed to initialize default certificate store", e);
        }
    }

    private static void ensureEngine(final SqSSL ssl) {
        if (ssl.serverName != null && !MiscUtils.isBlank(ssl.serverName)) {
            ssl.engine = ssl.context.createSSLEngine(ssl.serverName, -1);
        } else {
            ssl.engine = ssl.context.createSSLEngine();
        }

        ssl.engine.setEnabledProtocols(ENABLED_PROTOCOLS);
        ssl.buffer = ByteBuffer.allocate(getBufferSize(ssl));
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecrypt")
    protected abstract static class PrimDecryptNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        /**
         * Takes incoming data for decryption and continues to decrypt data.
         *
         * <p>
         * A global buffer is required, which retains state across multiple calls to this primitive,
         * for the following two reasons: First, the source buffer may contain arbitrarily split SSL
         * packets. Raw net data is passed in as it arrives; re-assembly of encrypted data is left
         * to the SSL plugin. Second, this primitive is not supposed to interpret the entire source
         * buffer, and is best thought of as FIFO queue. The primitive must copy the entire source
         * buffer into the global buffer, and must only decrypt one SSL packet at a time. The
         * primitive can be called with an empty source buffer, in order to drain the global buffer
         * one-by-one.
         * </p>
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param sourceBuffer the source buffer containing encrypted data
         * @param start the one-based start index
         * @param length the length of the data to interpret in the source buffer
         * @param targetBuffer the target buffer
         * @return the number of bytes produced in the output buffer
         */
        @TruffleBoundary
        @Specialization(guards = {"sourceBuffer.isByteType()", "targetBuffer.isByteType()"})
        protected final long doDecrypt(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSLOrNull(sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            /**
             * Grow buffer if it is not large enough for the source to avoid
             * {@link BufferOverflowException}s.
             */
            if (length > ssl.buffer.limit() - ssl.buffer.position()) {
                assert ssl.buffer.position() > 0 : "Only observed this";
                final ByteBuffer newBuffer = ByteBuffer.allocate(ssl.buffer.limit() + (int) length);
                ssl.buffer.flip();
                newBuffer.put(ssl.buffer);
                ssl.buffer = newBuffer;
            }

            try {
                ssl.buffer.put(source);
                decryptOne(ssl, target);
                return target.position();
            } catch (final BufferOverflowException | SSLException e) {
                getContext().printToStdErr(e);
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        private static void decryptOne(final SqSSL ssl, final ByteBuffer target) throws SSLException {
            ssl.buffer.flip();
            final SSLEngineResult result = unwrap(ssl, ssl.buffer, target);
            checkStatus("Decrypt status", result, Status.OK, Status.BUFFER_UNDERFLOW, Status.CLOSED);

            if (result.getStatus() == Status.OK || result.getStatus() == Status.BUFFER_UNDERFLOW) {
                ssl.buffer.compact();
            }

            if (result.getStatus() == Status.CLOSED) {
                connectionClosed(ssl);
            }
        }

        // FIXME recheck close event - is it indeed possible to connect again?
        private static void connectionClosed(final SqSSL ssl) {
            ssl.buffer.clear();
            ssl.state = State.UNUSED;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveEncrypt")
    protected abstract static class PrimEncryptNode extends AbstractPrimitiveNode implements Primitive5WithFallback {
        /**
         * Encrypt the incoming buffer into the result buffer.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param sourceBuffer the source buffer containing the plaintext
         * @param start the one-based start index
         * @param length the length of the data to interpret in the source buffer
         * @param targetBuffer the target buffer, will contain encrypted data
         * @return the number of bytes produced as a result
         */
        @Specialization(guards = {"sourceBuffer.isByteType()", "targetBuffer.isByteType()"})
        protected final long doEncrypt(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSLOrNull(sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                encrypt(ssl, source, target);
                return target.position();
            } catch (final SSLException e) {
                getContext().printToStdErr(e);
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        private static void encrypt(final SqSSL ssl, final ByteBuffer source, final ByteBuffer target) throws SSLException {
            final SSLEngineResult result = wrap(ssl, source, target);
            checkStatus("Encrypt status", result, Status.OK);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetIntProperty")
    protected abstract static class PrimGetIntPropertyNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        /**
         * Returns an integer property from an SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the ID of the property, see {@link IntProperty}
         * @return despite return code convention, non-zero if successful
         */
        @Specialization
        protected static final long doGet(@SuppressWarnings("unused") final Object receiver, final PointersObject sslHandle, final long propertyId,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqSSL ssl = getSSLOrNull(sslHandle);
            final IntProperty property = propertyWithId(IntProperty.class, propertyId);

            if (ssl == null || property == null) {
                errorProfile.enter(node);
                return 0L;
            }

            return switch (property) {
                case SSL_STATE -> ssl.state.id();
                case VERSION -> Constants.VERSION;
                case CERTIFICATE_STATE -> 0L; // FIXME
                default -> 0L;
            };
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetIntProperty")
    protected abstract static class PrimSetIntPropertyNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        /**
         * Sets an integer property in a SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the property ID; see {@link IntProperty}
         * @param anInteger the property value
         * @return despite the return code convention, non-zero if successful
         */
        @Specialization
        protected static final long doSet(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final long propertyId,
                        final long anInteger,
                        @Bind final Node node,
                        @Cached final InlinedBranchProfile errorProfile) {
            final SqSSL ssl = getSSLOrNull(sslHandle);
            final IntProperty property = propertyWithId(IntProperty.class, propertyId);
            if (ssl == null || property == null) {
                errorProfile.enter(node);
                return 0L;
            }

            if (property == IntProperty.LOG_LEVEL) {
                ssl.logLevel = anInteger;
            }

            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetStringProperty")
    protected abstract static class PrimGetStringPropertyNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        /**
         * Returns a string property from a SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the property ID; see {@link StringProperty}
         * @return the property value or {@code nil}
         */
        @Specialization
        protected final AbstractSqueakObject doGet(@SuppressWarnings("unused") final Object receiver, final PointersObject sslHandle, final long propertyId) {
            final SqSSL impl = getSSLOrNull(sslHandle);
            final StringProperty property = propertyWithId(StringProperty.class, propertyId);
            if (impl == null || property == null) {
                return NilObject.SINGLETON;
            }

            return getStringPropertyValue(getContext(), impl, property);
        }

        private static AbstractSqueakObject getStringPropertyValue(final SqueakImageContext image, final SqSSL impl, final StringProperty property) {
            return switch (property) {
                case PEER_NAME -> image.asByteString(impl.peerName);
                case CERTIFICATE_NAME -> NilObject.SINGLETON; // FIXME
                case SERVER_NAME -> image.asByteString(impl.serverName);
            };
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetStringProperty")
    protected abstract static class PrimSetStringPropertyNode extends AbstractPrimitiveNode implements Primitive3WithFallback {
        /**
         * Sets a string property in a SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the property ID; see {@link StringProperty}
         * @param aString the property value to set
         * @return despite return code convention, non-zero if successful
         */
        @Specialization(guards = "aString.isByteType()")
        protected static final long doSet(@SuppressWarnings("unused") final Object receiver,
                        final PointersObject sslHandle,
                        final long propertyId,
                        final NativeObject aString) {

            final SqSSL ssl = getSSLOrNull(sslHandle);
            final StringProperty property = propertyWithId(StringProperty.class, propertyId);
            if (ssl == null || property == null) {
                return 0L;
            }

            final String value = aString.asStringUnsafe();

            return switch (property) {
                case CERTIFICATE_NAME -> {
                    certificateName = value;
                    yield 1L;
                }
                case SERVER_NAME -> {
                    ssl.serverName = value;
                    yield 1L;
                }
                default -> 0L;
            };
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCreate")
    protected abstract static class PrimCreateNode extends AbstractPrimitiveNode implements Primitive0 {
        /**
         * Creates and returns a new SSL handle.
         *
         * @param receiver the receiver
         * @return a pointer to the newly created SSL instance
         */
        @Specialization
        protected final PointersObject doCreate(@SuppressWarnings("unused") final Object receiver) {
            return PointersObject.newHandleWithHiddenObject(getContext(), new SqSSL());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDestroy")
    protected abstract static class PrimDestroyNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        /**
         * Destroys the SSL session handle.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @return despite return code convention, non-zero if successful
         */
        @Specialization
        protected static final long doDestroy(@SuppressWarnings("unused") final Object receiver, final PointersObject sslHandle) {
            if (getSSLOrNull(sslHandle) == null) {
                return 0L;
            } else {
                sslHandle.setHiddenObject(null);
                return 1L;
            }
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakSSLFactory.getFactories();
    }
}
