/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.shared;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import sun.nio.cs.ThreadLocalCoders;

public final class LogHandlerAccessor {

    private static final int GIG = (int) Math.pow(1024, 3);

    public static Handler createLogHandler(final String mode) {
        PrintStream output = null;
        switch (mode) {
            case "mapped":
                return new MappedHandler();
            case "file":
                return new FileStreamHandler();
            case "err":
                output = System.err;
                break;
            case "out":
            default:
                output = System.out;
        }
        return new StandardPrintStreamHandler(output);
    }

    protected static Path getLogPath() {
        return Paths.get(System.currentTimeMillis() + ".log");
    }

    private static final class MappedHandler extends Handler {
        private MappedByteBuffer buffer;
        private Path path;
        private PrintStream originalOut = System.out;
        private PrintStream originalErr = System.err;

        private MappedHandler() {
            initializeMappedBuffer();
        }

        private void initializeMappedBuffer() {
            path = getLogPath();
            try (FileChannel channel = FileChannel.open(path, CREATE_NEW, READ, WRITE)) {
                buffer = channel.map(MapMode.READ_WRITE, 0, GIG);
                final PrintStream ps = new PrintStream(
                                new OutputStream() {
                                    @Override
                                    public void write(final int b) throws IOException {
                                        if (buffer.position() + 1 >= GIG) {
                                            close();
                                            initializeMappedBuffer();
                                        }
                                        buffer.put((byte) b);
                                    }
                                },
                                true) {
                    @Override
                    public void println() {
                        if (buffer.position() + 1 >= GIG) {
                            close();
                            initializeMappedBuffer();
                        }
                        buffer.put((byte) 10);
                    }

                    @Override
                    public void println(final boolean x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final char x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final int x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final long x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final float x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final double x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final char[] x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final Object x) {
                        println(String.valueOf(x));
                    }

                    @Override
                    public void println(final String x) {
                        final CharsetEncoder encoder = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8);
                        if (buffer.position() + 1 + x.length() * encoder.maxBytesPerChar() >= GIG) {
                            close();
                            initializeMappedBuffer();
                        }
                        encoder.encode(CharBuffer.wrap(x), buffer, true);
                        encoder.flush(buffer);
                        buffer.put((byte) 10);
                    }
                };
                System.setOut(ps);
                System.setErr(ps);
            } catch (final IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public void publish(final LogRecord record) {
            final CharsetEncoder encoder = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8);
            final String message = record.getMessage();
            if (buffer.position() + 1 + message.length() * encoder.maxBytesPerChar() >= GIG) {
                close();
                initializeMappedBuffer();
            }
            encoder.encode(CharBuffer.wrap(message), buffer, true);
            encoder.flush(buffer);
            buffer.put((byte) 10);
        }

        @Override
        public void flush() {
            buffer.limit(buffer.position());
            buffer.force();
        }

        @Override
        public void close() throws SecurityException {
            final int position = buffer.position();
            buffer.limit(position);
            buffer.force();
            MappedBufferCleaner.closeDirectByteBuffer(buffer);
            buffer = null;  // let it be garbage collected
            System.setOut(originalOut);
            System.setErr(originalErr);
            try (FileChannel channel = FileChannel.open(path, READ, WRITE)) {
                channel.truncate(position);
                if (position == 0) {
                    path.getFileSystem().provider().delete(path);
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     *
     * Cleaner code courtesy of Luke Hutchison via a StackOverflow answer.
     * https://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-
     * filechannel-in-java
     *
     */
    private static class MappedBufferCleaner {
        private static final boolean PRE_JAVA_9 = System.getProperty("java.specification.version", "9").startsWith("1.");

        private static Method cleanMethod;
        private static Method attachmentMethod;
        private static Object theUnsafe;

        static void getCleanMethodPrivileged() {
            if (PRE_JAVA_9) {
                try {
                    cleanMethod = Class.forName("sun.misc.Cleaner").getMethod("clean");
                    cleanMethod.setAccessible(true);
                    final Class<?> directByteBufferClass = Class.forName("sun.nio.ch.DirectBuffer");
                    attachmentMethod = directByteBufferClass.getMethod("attachment");
                    attachmentMethod.setAccessible(true);
                } catch (final Exception ex) {
                }
            } else {
                try {
                    Class<?> unsafeClass;
                    try {
                        unsafeClass = Class.forName("sun.misc.Unsafe");
                    } catch (final Exception e) {
                        // jdk.internal.misc.Unsafe doesn't yet have invokeCleaner(),
                        // but that method should be added if sun.misc.Unsafe is removed.
                        unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                    }
                    cleanMethod = unsafeClass.getMethod("invokeCleaner", ByteBuffer.class);
                    cleanMethod.setAccessible(true);
                    final Field theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
                    theUnsafeField.setAccessible(true);
                    theUnsafe = theUnsafeField.get(null);
                } catch (final Exception ex) {
                }
            }
        }

        static {
            AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                getCleanMethodPrivileged();
                return null;
            });
        }

        private static boolean closeDirectByteBufferPrivileged(
                        final ByteBuffer byteBuffer) {
            try {
                if (cleanMethod == null) {
                    println("Could not unmap ByteBuffer, cleanMethod == null");
                    return false;
                }
                if (PRE_JAVA_9) {
                    if (attachmentMethod == null) {
                        println("Could not unmap ByteBuffer, attachmentMethod == null");
                        return false;
                    }
                    // Make sure duplicates and slices are not cleaned, since this can result in
                    // duplicate attempts to clean the same buffer, which trigger a crash with:
                    // "A fatal error has been detected by the Java Runtime Environment:
                    // EXCEPTION_ACCESS_VIOLATION"
                    // See: https://stackoverflow.com/a/31592947/3950982
                    if (attachmentMethod.invoke(byteBuffer) != null) {
                        // Buffer is a duplicate or slice
                        return false;
                    }
                    // Invoke ((DirectBuffer) byteBuffer).cleaner().clean()
                    final Method cleaner = byteBuffer.getClass().getMethod("cleaner");
                    cleaner.setAccessible(true);
                    cleanMethod.invoke(cleaner.invoke(byteBuffer));
                    return true;
                } else {
                    if (theUnsafe == null) {
                        println("Could not unmap ByteBuffer, theUnsafe == null");
                        return false;
                    }
                    // In JDK9+, calling the above code gives a reflection warning on stderr,
                    // need to call Unsafe.theUnsafe.invokeCleaner(byteBuffer) , which makes
                    // the same call, but does not print the reflection warning.
                    try {
                        cleanMethod.invoke(theUnsafe, byteBuffer);
                        return true;
                    } catch (final IllegalArgumentException e) {
                        // Buffer is a duplicate or slice
                        return false;
                    }
                }
            } catch (final Exception e) {
                println("Could not unmap ByteBuffer: " + e);
                return false;
            }
        }

        /**
         * Close a {@code DirectByteBuffer} -- in particular, will unmap a {@link MappedByteBuffer}.
         *
         * @param byteBuffer The {@link ByteBuffer} to close/unmap.
         * @return True if the byteBuffer was closed/unmapped (or if the ByteBuffer was null or
         *         non-direct).
         */
        public static boolean closeDirectByteBuffer(final ByteBuffer byteBuffer) {
            if (byteBuffer != null && byteBuffer.isDirect()) {
                return AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> closeDirectByteBufferPrivileged(byteBuffer));
            } else {
                // Nothing to unmap
                return false;
            }
        }
    }

    private static final class FileStreamHandler extends Handler {

        private OutputStream stream;
        private FileChannel channel;

        private FileStreamHandler() {
            initializeOutputStream();
        }

        private void initializeOutputStream() {
            try {
                channel = FileChannel.open(getLogPath(), CREATE_NEW, READ, WRITE);
                stream = new BufferedOutputStream(Channels.newOutputStream(channel), 65536);
            } catch (final IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public void publish(final LogRecord record) {
            final String message = record.getMessage();
            final CharsetEncoder encoder = ThreadLocalCoders.encoderFor(StandardCharsets.UTF_8);
            final ByteBuffer buffer = ByteBuffer.allocate((int) (message.length() * encoder.maxBytesPerChar()));
            encoder.encode(CharBuffer.wrap(message), buffer, true);
            encoder.flush(buffer);
            try {
                if (channel.position() + 65536 + buffer.position() >= GIG) {
                    close();
                    initializeOutputStream();
                }
                stream.write(Arrays.copyOf(buffer.array(), buffer.position()));
                stream.write((byte) 10);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void flush() {
            try {
                stream.flush();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() throws SecurityException {
            try {
                stream.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static final class StandardPrintStreamHandler extends Handler {

        private PrintStream stream;

        private StandardPrintStreamHandler(final PrintStream stream) {
            this.stream = stream;
        }

        @Override
        public void publish(final LogRecord record) {
            stream.println(record.getMessage());
        }

        @Override
        public void flush() {
            stream.flush();
        }

        @Override
        public void close() throws SecurityException {
            // do nothing
        }
    }

    private static void println(final String string) {
        // Checkstyle: stop
        System.out.println(string);
        // Checkstyle: resume
    }
}
