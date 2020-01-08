/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import static java.util.Arrays.asList;

import java.io.IOException;
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
import javax.net.ssl.SSLSession;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.SenaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

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
 * @see sun.security.ssl.Debug
 */
public final class SqueakSSL extends AbstractPrimitiveFactoryHolder {
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

    public static final class SqSSL {
        private State state = State.UNUSED;
        private SSLContext context;
        private SSLEngine engine;

        private String peerName;
        private String serverName;

        private ByteBuffer buffer;

        @SuppressWarnings("unused" /* TODO */) private long logLevel;
    }

    @TruffleBoundary
    private static SqSSL getSSL(final CompiledMethodObject method, final long handle) {
        return method.image.squeakSSLHandles.get(handle);
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
                case BUFFER_UNDERFLOW:
                    return result;

                case BUFFER_OVERFLOW:
                    intermediateTarget = enlargeBufferFrom(intermediateTarget, bufferSize);
                    continue;

                case OK:
                    intermediateTarget.flip();
                    if (intermediateTarget.remaining() > 0) {
                        targetBuffer.put(intermediateTarget);
                    }
                    return result;

                case CLOSED:
                    intermediateTarget.flip();
                    targetBuffer.put(intermediateTarget);
                    return result;

                default:
                    throw SqueakException.create("Unknown SSL engine status");
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
    private static ByteBuffer asReadBuffer(final NativeObject buffer, final long start, final long length) {
        return ByteBuffer.wrap(buffer.getByteStorage(), (int) start - 1, (int) length).asReadOnlyBuffer();
    }

    /**
     * @param buffer the Squeak buffer object (byte type)
     * @return a write-through byte buffer
     */
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
        final List<HandshakeStatus> expectedList = asList(expected);
        assert !expectedList.contains(HandshakeStatus.FINISHED) : "State FINISHED is never returned from engine";
        final HandshakeStatus actual = ssl.engine.getHandshakeStatus();
        if (!expectedList.contains(actual)) {
            throw new IllegalStateException(String.format("Handshake status %s expected, actual: %s. %s",
                            Arrays.toString(expected), actual, message));
        }
    }

    @TruffleBoundary
    private static void checkHandshake(final String message, final SSLEngineResult result, final HandshakeStatus... expected) {
        final HandshakeStatus actual = result.getHandshakeStatus();
        if (!asList(expected).contains(actual)) {
            throw new IllegalStateException(String.format("Handshake status %s expected, actual: %s. %s",
                            Arrays.toString(expected), actual, message));
        }
    }

    @TruffleBoundary
    private static void checkStatus(final String message, final SSLEngineResult result, final SSLEngineResult.Status... expected) {
        final SSLEngineResult.Status actual = result.getStatus();
        if (!asList(expected).contains(actual)) {
            throw new IllegalStateException(String.format("Status %s expected, actual: %s. %s", Arrays.toString(expected), actual, message));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveAccept")
    protected abstract static class PrimAcceptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimAcceptNode(final CompiledMethodObject method) {
            super(method);
        }

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
                        final long sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSL(method, sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                return process(ssl, source, target);
            } catch (final SSLException e) {
                e.printStackTrace(method.image.getError());
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        private static long process(final SqSSL ssl, final ByteBuffer source, final ByteBuffer target) throws SSLException {
            if (ssl.state == State.UNUSED) {
                ssl.state = State.ACCEPTING;
                setUp(ssl);
                ssl.engine.setUseClientMode(false);
            }

            if (ssl.state == State.ACCEPTING) {
                ssl.buffer.put(source);
                unwrapEagerly(ssl);
                wrapEagerly(ssl, target);
                return target.position();
            }

            return ReturnCode.INVALID_STATE.id();
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
    protected abstract static class PrimConnectNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimConnectNode(final CompiledMethodObject method) {
            super(method);
        }

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
                        final long sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSL(method, sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                return processHandshake(ssl, source, target);
            } catch (final SSLException e) {
                e.printStackTrace(method.image.getError());
                return ReturnCode.GENERIC_ERROR.id();
            }
        }

        private static long processHandshake(final SqSSL ssl, final ByteBuffer source, final ByteBuffer target) throws SSLException {
            if (ssl.state == State.UNUSED) {
                beginHandshake(ssl, target);
                return target.position();
            } else if (ssl.state == State.CONNECTING) {
                ssl.buffer.put(source);
                readHandshakeResponse(ssl);
                writeHandshakeResponse(ssl, target);
                return target.position();
            } else {
                return ReturnCode.INVALID_STATE.id();
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
            ssl.peerName = ssl.engine.getPeerHost();
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
        if (certificateName != null && !certificateName.trim().isEmpty()) {
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
            throw SqueakException.create("Failed to load certificate " + certificate + ". Does the file exist?", e);
        } catch (final GeneralSecurityException e) {
            throw SqueakException.create("Security error when loading certificate " + certificate, e);
        }
    }

    private static void initializeWithDefaultCertificates(final SqSSL ssl) {
        try {
            ssl.context = SSLContext.getInstance("TLS");
            ssl.context.init(null, null, null);
        } catch (final GeneralSecurityException e) {
            throw SqueakException.create("Failed to initialize default certificate store", e);
        }
    }

    private static void ensureEngine(final SqSSL ssl) {
        if (ssl.serverName != null && !ssl.serverName.trim().isEmpty()) {
            ssl.engine = ssl.context.createSSLEngine(ssl.serverName, -1);
        } else {
            ssl.engine = ssl.context.createSSLEngine();
        }

        // On JDK 11, TLS 1.3 would be selected. However, this does not seem to be operational.
        // After exchanging ClientHello and ServerHello, the client instance writes 6 bytes,
        // the "Client Change Cipher Spec", which the server can successfully read.
        // Then both engines end up in "NEED_UNWRAP" state (expecting to be fed new input),
        // however, both engines refuse to produce new data.
        // TLS 1.3 details: https://tls13.ulfheim.net/
        ssl.engine.setEnabledProtocols(new String[]{"TLSv1.2"});
        ssl.buffer = ByteBuffer.allocate(getBufferSize(ssl));
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDecrypt")
    protected abstract static class PrimDecryptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimDecryptNode(final CompiledMethodObject method) {
            super(method);
        }

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
        @Specialization(guards = {"sourceBuffer.isByteType()", "targetBuffer.isByteType()"})
        protected final long doDecrypt(@SuppressWarnings("unused") final Object receiver,
                        final long sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSL(method, sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                ssl.buffer.put(source);
                decryptOne(ssl, target);
                return target.position();
            } catch (final SSLException e) {
                e.printStackTrace(method.image.getError());
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
    protected abstract static class PrimEncryptNode extends AbstractPrimitiveNode implements SenaryPrimitive {
        protected PrimEncryptNode(final CompiledMethodObject method) {
            super(method);
        }

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
                        final long sslHandle,
                        final NativeObject sourceBuffer,
                        final long start,
                        final long length,
                        final NativeObject targetBuffer) {

            final SqSSL ssl = getSSL(method, sslHandle);
            if (ssl == null) {
                return ReturnCode.INVALID_STATE.id();
            }

            final ByteBuffer source = asReadBuffer(sourceBuffer, start, length);
            final ByteBuffer target = asWriteBuffer(targetBuffer);

            try {
                encrypt(ssl, source, target);
                return target.position();
            } catch (final SSLException e) {
                e.printStackTrace(method.image.getError());
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
    protected abstract static class PrimGetIntPropertyNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimGetIntPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Returns an integer property from an SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the ID of the property, see {@link IntProperty}
         * @return despite return code convention, non-zero if successful
         */
        @Specialization
        protected long doGet(@SuppressWarnings("unused") final Object receiver, final long sslHandle, final long propertyId) {
            final SqSSL ssl = getSSL(method, sslHandle);
            final IntProperty property = propertyWithId(IntProperty.class, propertyId);

            if (ssl == null || property == null) {
                return 0L;
            }

            switch (property) {
                case SSL_STATE:
                    return ssl.state.id();
                case VERSION:
                    return Constants.VERSION;
                case CERTIFICATE_STATE:
                    // FIXME
                    return 0L;
                default:
                    return 0L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetIntProperty")
    protected abstract static class PrimSetIntPropertyNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSetIntPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected long doSet(@SuppressWarnings("unused") final Object receiver,
                        final long sslHandle,
                        final long propertyId,
                        final long anInteger) {
            final SqSSL ssl = getSSL(method, sslHandle);
            final IntProperty property = propertyWithId(IntProperty.class, propertyId);
            if (ssl == null || property == null) {
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
    protected abstract static class PrimGetStringPropertyNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimGetStringPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Returns a string property from a SSL session.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @param propertyId the property ID; see {@link StringProperty}
         * @return the property value or {@code nil}
         */
        @Specialization
        protected final AbstractSqueakObject doGet(@SuppressWarnings("unused") final Object receiver, final long sslHandle, final long propertyId) {
            final SqSSL impl = getSSL(method, sslHandle);
            final StringProperty property = propertyWithId(StringProperty.class, propertyId);
            if (impl == null || property == null) {
                return NilObject.SINGLETON;
            }

            final String value = getStringPropertyValue(impl, property);
            return value == null ? NilObject.SINGLETON : method.image.asByteString(value);
        }

        private static String getStringPropertyValue(final SqSSL impl, final StringProperty property) {
            switch (property) {
                case PEER_NAME:
                    return impl.peerName == null ? "" : impl.peerName;
                case CERTIFICATE_NAME:
                    return null; // FIXME
                case SERVER_NAME:
                    return impl.serverName;
                default:
                    return null;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetStringProperty")
    protected abstract static class PrimSetStringPropertyNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimSetStringPropertyNode(final CompiledMethodObject method) {
            super(method);
        }

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
        protected long doSet(@SuppressWarnings("unused") final Object receiver,
                        final long sslHandle,
                        final long propertyId,
                        final NativeObject aString) {

            final SqSSL ssl = getSSL(method, sslHandle);
            final StringProperty property = propertyWithId(StringProperty.class, propertyId);
            if (ssl == null || property == null) {
                return 0L;
            }

            final String value = aString.asStringUnsafe();

            switch (property) {
                case CERTIFICATE_NAME:
                    certificateName = value;
                    break;

                case SERVER_NAME:
                    ssl.serverName = value;
                    break;

                default:
                    return 0L;
            }

            return 1L;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCreate")
    protected abstract static class PrimCreateNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimCreateNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Creates and returns a new SSL handle.
         *
         * @param receiver the receiver
         * @return a pointer to the newly created SSL instance
         */
        @Specialization
        @TruffleBoundary
        protected long doCreate(@SuppressWarnings("unused") final Object receiver) {
            final SqSSL ssl = new SqSSL();
            final long handle = ssl.hashCode();
            method.image.squeakSSLHandles.put(handle, ssl);
            return handle;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDestroy")
    protected abstract static class PrimDestroyNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimDestroyNode(final CompiledMethodObject method) {
            super(method);
        }

        /**
         * Destroys the SSL session handle.
         *
         * @param receiver the receiver
         * @param sslHandle the handle of the target SSL instance
         * @return despite return code convention, non-zero if successful
         */
        @Specialization
        @TruffleBoundary
        protected long doDestroy(@SuppressWarnings("unused") final Object receiver, final long sslHandle) {
            final SqSSL ssl = method.image.squeakSSLHandles.removeKey(sslHandle);
            if (ssl == null) {
                return 0L;
            } else {
                return 1L;
            }
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SqueakSSLFactory.getFactories();
    }
}
