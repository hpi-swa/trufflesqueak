package de.hpi.swa.graal.squeak.util;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class SqueakDisplay {
    @CompilationFinal private static final String DEFAULT_WINDOW_TITLE = "GraalSqueak";
    @CompilationFinal public static final int CURSOR_WIDTH = 16;
    @CompilationFinal public static final int CURSOR_HEIGHT = 16;

    public static final class EVENT_TYPE {
        public static final long NONE = 0;
        public static final long MOUSE = 1;
        public static final long KEYBOARD = 2;
        public static final long DRAG_DROP_FILES = 3;
        public static final long MENU = 4;
        public static final long WINDOW = 5;
        public static final long COMPLEX = 6;
        public static final long MOUSE_WHEEL = 7;
    }

    public static final int EVENT_SIZE = 8;

    private static final long[] NULL_EVENT = new long[]{EVENT_TYPE.NONE, 0, 0, 0, 0, 0, 0, 0};

    public static AbstractSqueakDisplay create(final SqueakImageContext image, final boolean noDisplay) {
        if (!GraphicsEnvironment.isHeadless() && !noDisplay) {
            return new JavaDisplay(image);
        } else {
            return new NullDisplay();
        }
    }

    public abstract static class AbstractSqueakDisplay {
        public abstract void forceRect(int left, int right, int top, int bottom);

        public abstract Dimension getSize();

        public abstract void setFullscreen(boolean enable);

        public abstract void forceUpdate();

        public abstract void open();

        public abstract void close();

        public abstract void setSqDisplay(PointersObject sqDisplay);

        public abstract Point getLastMousePosition();

        public abstract int getLastMouseButton();

        public abstract int keyboardPeek();

        public abstract int keyboardNext();

        public abstract boolean isHeadless();

        public abstract void setCursor(int[] cursorWords, int depth);

        public abstract long[] getNextEvent();

        public abstract void setDeferUpdates(boolean flag);

        public abstract void adjustDisplay(long depth, long width, long height, boolean fullscreen);

        public abstract void resizeTo(int width, int height);

        public abstract void setWindowTitle(String title);
    }

    public static final class JavaDisplay extends AbstractSqueakDisplay {
        @CompilationFinal public final SqueakImageContext image;
        @CompilationFinal private final JFrame frame = new JFrame(DEFAULT_WINDOW_TITLE);
        @CompilationFinal private final Canvas canvas = new Canvas();

        @CompilationFinal public final SqueakMouse mouse;
        @CompilationFinal public final SqueakKeyboard keyboard;
        @CompilationFinal private final Deque<long[]> deferredEvents = new ArrayDeque<>();

        @CompilationFinal private static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
        @CompilationFinal(dimensions = 1) private static final byte[] BLACK_AND_WHITE = new byte[]{(byte) 255, (byte) 0};
        @CompilationFinal(dimensions = 1) private static final byte[] ALPHA_COMPONENT = new byte[]{(byte) 0, (byte) 255};
        @CompilationFinal private static final ColorModel CURSOR_MODEL = new IndexColorModel(1, 2, BLACK_AND_WHITE, BLACK_AND_WHITE, BLACK_AND_WHITE, ALPHA_COMPONENT);

        private Dimension windowSize = null;
        private boolean deferUpdates = false;

        public JavaDisplay(final SqueakImageContext image) {
            this.image = image;
            mouse = new SqueakMouse(this);
            keyboard = new SqueakKeyboard(this);

            // install event listeners
            canvas.addMouseListener(mouse);
            canvas.addMouseMotionListener(mouse);
            frame.addKeyListener(keyboard);

            frame.setTitle(DEFAULT_WINDOW_TITLE + " (" + image.config.getImagePath() + ")");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(canvas);
            frame.setResizable(true);
            frame.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(final ComponentEvent evt) {
                    canvas.resizeTo(frame.getSize());
                }
            });
        }

        private final class Canvas extends JComponent {
            private static final long serialVersionUID = 1L;
            @CompilationFinal private BufferedImage bufferedImage;
            @CompilationFinal private NativeObject bitmap;
            @CompilationFinal private int width;
            @CompilationFinal private int height;
            @CompilationFinal private int depth;

            @Override
            public void paintComponent(final Graphics g) {
                if (deferUpdates || bitmap == null) {
                    return;
                }
                //@formatter:off
                switch (depth) {
                    case 1: case 2: case 4: case 8: // colors need to be decoded
                        bufferedImage.setRGB(0, 0, width, height, decodeColors(), 0, width);
                        break;
                    case 16:
                        bufferedImage.setData(get16bitRaster());
                        break;
                    case 32: // use words directly
                        assert bitmap.getWords().length / width / height == 1;
                        final int drawWidth = Math.min(width, frame.getWidth());
                        final int drawHeight = Math.min(height, frame.getHeight());
                        bufferedImage.setRGB(0, 0, drawWidth, drawHeight, bitmap.getWords(), 0, width);
                        break;
                    default:
                        throw new SqueakException("Unsupported form depth: " + depth);
                }
                //@formatter:on
                g.drawImage(bufferedImage, 0, 0, null);
            }

            private int[] decodeColors() {
                final int shift = 4 - depth;
                int pixelmask = ((1 << depth) - 1) << shift;
                final int[] table = SqueakPixelLookup.TABLE[depth - 1];
                final int[] words = bitmap.getWords();
                final int[] rgb = new int[words.length];
                for (int i = 0; i < words.length; i++) {
                    final int pixel = (words[i] & pixelmask) >> (shift - i * depth);
                    rgb[i] = table[pixel];
                    pixelmask >>= depth;
                }
                return rgb;
            }

            private Raster get16bitRaster() {
                final int[] words = bitmap.getWords();
                assert words.length * 2 / width / height == 1;
                final DirectColorModel colorModel = new DirectColorModel(16,
                                0x001f, // red
                                0x03e0, // green
                                0x7c00, // blue
                                0x8000  // alpha
                );
                final WritableRaster raster = colorModel.createCompatibleWritableRaster(width, height);
                int word;
                int high;
                int low;
                int x;
                int y;
                Object pixel = null;
                for (int i = 0; i < words.length; i++) {
                    word = words[i];
                    high = word >> 16;
                    low = word & 0xffff;
                    x = (i % width / 2) * 2;
                    y = i / width;
                    pixel = colorModel.getDataElements(high, pixel);
                    raster.setDataElements(x, y, pixel);
                    pixel = colorModel.getDataElements(low, pixel);
                    raster.setDataElements(x + 1, y, pixel);
                }
                return raster;
            }

            private void resizeTo(final Dimension newSize) {
                setSize(newSize);
                CompilerDirectives.transferToInterpreterAndInvalidate();
                bufferedImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                repaint();
            }

            private void setSqDisplay(final PointersObject sqDisplay) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                this.bitmap = (NativeObject) sqDisplay.at0(FORM.BITS);
                this.width = ((Long) sqDisplay.at0(FORM.WIDTH)).intValue();
                this.height = ((Long) sqDisplay.at0(FORM.HEIGHT)).intValue();
                this.depth = ((Long) sqDisplay.at0(FORM.DEPTH)).intValue();
            }
        }

        @Override
        @TruffleBoundary
        public void forceRect(final int left, final int right, final int top, final int bottom) {
            canvas.repaint(left, top, right - left, bottom - top);
        }

        @Override
        @TruffleBoundary
        public void forceUpdate() {
            canvas.repaint();
        }

        @Override
        public void open() {
            if (!frame.isVisible()) {
                frame.setVisible(true);
                frame.requestFocus();
            }
        }

        @Override
        public void close() {
            frame.setVisible(false);
            frame.dispose();
        }

        @Override
        public Dimension getSize() {
            return frame.getSize();
        }

        @Override
        public void adjustDisplay(final long depth, final long width, final long height, final boolean fullscreen) {
            canvas.depth = ((Long) depth).intValue();
            canvas.width = ((Long) width).intValue();
            canvas.height = ((Long) height).intValue();
            frame.setSize(canvas.width, canvas.height);
            setFullscreen(fullscreen);
        }

        @Override
        public void resizeTo(final int width, final int height) {
            frame.setSize(width, height);
        }

        @Override
        public void setFullscreen(final boolean enable) {
            frame.dispose();
            frame.setUndecorated(enable);
            frame.setExtendedState(enable ? JFrame.MAXIMIZED_BOTH : JFrame.NORMAL);
            if (enable) {
                windowSize = frame.getSize();
            } else {
                frame.setPreferredSize(windowSize);
                frame.pack();
                frame.setLocationRelativeTo(null);
            }
            frame.setVisible(true);
        }

        @Override
        public void setSqDisplay(final PointersObject sqDisplay) {
            canvas.setSqDisplay(sqDisplay);
        }

        @Override
        public Point getLastMousePosition() {
            return mouse.getPosition();
        }

        @Override
        public int getLastMouseButton() {
            return mouse.getButtons() & 7 | keyboard.modifierKeys();
        }

        @Override
        public int keyboardPeek() {
            return keyboard.peekKey();
        }

        @Override
        public int keyboardNext() {
            return keyboard.nextKey();
        }

        @Override
        public boolean isHeadless() {
            return false;
        }

        @Override
        public void setCursor(final int[] cursorWords, final int depth) {
            final Dimension bestCursorSize = TOOLKIT.getBestCursorSize(CURSOR_WIDTH, CURSOR_HEIGHT);
            final Cursor cursor;
            if (bestCursorSize.width == 0 || bestCursorSize.height == 0) {
                cursor = Cursor.getDefaultCursor();
            } else {
                final DataBuffer buf = new DataBufferInt(cursorWords, (CURSOR_WIDTH * CURSOR_HEIGHT / 8) * depth);
                final SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, CURSOR_WIDTH, CURSOR_HEIGHT, depth);
                final WritableRaster raster = Raster.createWritableRaster(sm, buf, null);
                final Image cursorImage = new BufferedImage(CURSOR_MODEL, raster, false, null);
                cursor = TOOLKIT.createCustomCursor(cursorImage, new Point(0, 0), "GraalSqueak Cursor");
            }
            canvas.setCursor(cursor);
        }

        @Override
        public long[] getNextEvent() {
            if (!deferredEvents.isEmpty()) {
                return deferredEvents.removeFirst();
            }
            return NULL_EVENT;
        }

        public void addEvent(final long[] event) {
            deferredEvents.add(event);
        }

        public long getEventTime() {
            return System.currentTimeMillis() - image.startUpMillis;
        }

        @Override
        public void setDeferUpdates(final boolean flag) {
            deferUpdates = flag;
        }

        @Override
        public void setWindowTitle(final String title) {
            frame.setTitle(title);
        }
    }

    private static final class NullDisplay extends AbstractSqueakDisplay {
        @CompilationFinal private static final Dimension DEFAULT_DIMENSION = new Dimension(1024, 768);
        @CompilationFinal private static final Point NULL_POINT = new Point(0, 0);

        @Override
        public void forceRect(final int left, final int right, final int top, final int bottom) {
            // ignore
        }

        @Override
        public Dimension getSize() {
            return DEFAULT_DIMENSION;
        }

        @Override
        public void setFullscreen(final boolean enable) {
            // ignore
        }

        @Override
        public void forceUpdate() {
            // ignore
        }

        @Override
        public void open() {
            // ignore
        }

        @Override
        public void close() {
            // ignore
        }

        @Override
        public void setSqDisplay(final PointersObject sqDisplay) {
            // ignore
        }

        @Override
        public Point getLastMousePosition() {
            return NULL_POINT;
        }

        @Override
        public int getLastMouseButton() {
            return 0;
        }

        @Override
        public int keyboardPeek() {
            return 0;
        }

        @Override
        public int keyboardNext() {
            return 0;
        }

        @Override
        public boolean isHeadless() {
            return true;
        }

        @Override
        public void setCursor(final int[] cursorWords, final int depth) {
            // ignore
        }

        @Override
        public long[] getNextEvent() {
            return NULL_EVENT;
        }

        @Override
        public void setDeferUpdates(final boolean flag) {
            // ignore
        }

        @Override
        public void adjustDisplay(final long depth, final long width, final long height, final boolean fullscreen) {
            // ignore
        }

        @Override
        public void resizeTo(final int width, final int height) {
            // ignore
        }

        @Override
        public void setWindowTitle(final String title) {
            // ignore
        }
    }
}
