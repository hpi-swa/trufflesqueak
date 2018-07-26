package de.hpi.swa.graal.squeak.io;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
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

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class SqueakDisplayJFrame extends SqueakDisplay {
    private static final String DEFAULT_WINDOW_TITLE = "GraalSqueak";
    private static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(200, 150);
    private static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
    @CompilationFinal(dimensions = 1) private static final byte[] BLACK_AND_WHITE = new byte[]{(byte) 255, (byte) 0};
    @CompilationFinal(dimensions = 1) private static final byte[] ALPHA_COMPONENT = new byte[]{(byte) 0, (byte) 255};
    private static final ColorModel CURSOR_MODEL = new IndexColorModel(1, 2, BLACK_AND_WHITE, BLACK_AND_WHITE, BLACK_AND_WHITE, ALPHA_COMPONENT);

    public final SqueakImageContext image;
    private final JFrame frame = new JFrame(DEFAULT_WINDOW_TITLE);
    private final Canvas canvas = new Canvas();
    private final SqueakMouse mouse;
    private final SqueakKeyboard keyboard;
    private final Deque<long[]> deferredEvents = new ArrayDeque<>();

    @CompilationFinal public boolean usesEventQueue = false;
    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons = 0;
    private Dimension windowSize = null;
    private boolean deferUpdates = false;

    public SqueakDisplayJFrame(final SqueakImageContext image) {
        this.image = image;
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);

        // install event listeners
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        frame.addKeyListener(keyboard);

        frame.setTitle(SqueakDisplayJFrame.DEFAULT_WINDOW_TITLE + " (" + image.config.getImagePath() + ")");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(MINIMUM_WINDOW_SIZE);
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
            if (bitmap == null) {
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
                    final int[] words = bitmap.getIntStorage();
                    assert words.length / width / height == 1;
                    final int drawWidth = Math.min(width, frame.getWidth());
                    final int drawHeight = Math.min(height, frame.getHeight());
                    bufferedImage.setRGB(0, 0, drawWidth, drawHeight, words, 0, width);
                    break;
                default:
                    throw new SqueakException("Unsupported form depth:",  depth);
            }
            //@formatter:on
            g.drawImage(bufferedImage, 0, 0, null);
        }

        private int[] decodeColors() {
            final int shift = 4 - depth;
            int pixelmask = ((1 << depth) - 1) << shift;
            final int[] table = SqueakIOConstants.PIXEL_LOOKUP_TABLE[depth - 1];
            final int[] words = bitmap.getIntStorage();
            final int[] rgb = new int[words.length];
            for (int i = 0; i < words.length; i++) {
                final int pixel = (words[i] & pixelmask) >> (shift - i * depth);
                rgb[i] = table[pixel];
                pixelmask >>= depth;
            }
            return rgb;
        }

        private Raster get16bitRaster() {
            final int[] words = bitmap.getIntStorage();
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
            if (!bitmap.isIntType()) {
                throw new SqueakException("Display bitmap expected to be a words object");
            }
            this.width = (int) (long) sqDisplay.at0(FORM.WIDTH);
            this.height = (int) (long) sqDisplay.at0(FORM.HEIGHT);
            this.depth = (int) (long) sqDisplay.at0(FORM.DEPTH);
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
        if (deferUpdates) {
            canvas.repaint();
        }
    }

    @Override
    @TruffleBoundary
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
        return canvas.getSize();
    }

    @Override
    public void adjustDisplay(final long depth, final long width, final long height, final boolean fullscreen) {
        canvas.depth = (int) depth;
        canvas.width = (int) width;
        canvas.height = (int) height;
        setFullscreen(fullscreen);
    }

    @Override
    @TruffleBoundary
    public void resizeTo(final int width, final int height) {
        frame.getContentPane().setPreferredSize(new Dimension(width, height));
        frame.pack();
    }

    @Override
    @TruffleBoundary
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
    @TruffleBoundary
    public void setSqDisplay(final PointersObject sqDisplay) {
        canvas.setSqDisplay(sqDisplay);
    }

    @Override
    public Point getLastMousePosition() {
        return mouse.getPosition();
    }

    @Override
    public int getLastMouseButton() {
        return buttons;
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
    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int depth) {
        final Dimension bestCursorSize = TOOLKIT.getBestCursorSize(SqueakIOConstants.CURSOR_WIDTH, SqueakIOConstants.CURSOR_HEIGHT);
        final Cursor cursor;
        if (bestCursorSize.width == 0 || bestCursorSize.height == 0) {
            cursor = Cursor.getDefaultCursor();
        } else {
            final DataBuffer buf = new DataBufferInt(cursorWords, (SqueakIOConstants.CURSOR_WIDTH * SqueakIOConstants.CURSOR_HEIGHT / 8) * depth);
            final SampleModel sm = new MultiPixelPackedSampleModel(DataBuffer.TYPE_INT, SqueakIOConstants.CURSOR_WIDTH, SqueakIOConstants.CURSOR_HEIGHT, depth);
            final WritableRaster raster = Raster.createWritableRaster(sm, buf, null);
            final Image cursorImage = new BufferedImage(CURSOR_MODEL, raster, false, null);
            cursor = TOOLKIT.createCustomCursor(cursorImage, new Point(0, 0), "GraalSqueak Cursor");
        }
        canvas.setCursor(cursor);
    }

    @Override
    @TruffleBoundary
    public long[] getNextEvent() {
        if (!deferredEvents.isEmpty()) {
            return deferredEvents.removeFirst();
        }
        if (!usesEventQueue) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            usesEventQueue = true;
        }
        return SqueakIOConstants.NULL_EVENT;
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        deferredEvents.add(new long[]{eventType, getEventTime(), value3, value4, value5, value6});
        if (inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }

    public int recordModifiers(final InputEvent e) {
        final int shiftValue = e.isShiftDown() ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = e.isControlDown() && !e.isAltDown() ? KEYBOARD.CTRL : 0;
        final int cmdValue = e.isMetaDown() || (e.isAltDown() && !e.isControlDown()) ? KEYBOARD.CMD : 0;
        final int modifiers = shiftValue + ctrlValue + cmdValue;
        buttons = (buttons & ~KEYBOARD.ALL) | modifiers;
        return modifiers;
    }

    public long getEventTime() {
        return System.currentTimeMillis() - image.startUpMillis;
    }

    @Override
    public void setDeferUpdates(final boolean flag) {
        deferUpdates = flag;
    }

    @Override
    @TruffleBoundary
    public void setWindowTitle(final String title) {
        frame.setTitle(title);
    }

    @Override
    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.inputSemaphoreIndex = interruptSemaphoreIndex;
    }
}
