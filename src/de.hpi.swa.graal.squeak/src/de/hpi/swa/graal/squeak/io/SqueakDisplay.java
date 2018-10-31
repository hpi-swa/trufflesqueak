package de.hpi.swa.graal.squeak.io;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JFrame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class SqueakDisplay implements SqueakDisplayInterface {
    private static final String DEFAULT_WINDOW_TITLE = "GraalSqueak";
    private static final boolean REPAINT_AUTOMATICALLY = false; // For debugging purposes.
    private static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(200, 150);
    private static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
    @CompilationFinal(dimensions = 1) private static final int[] CURSOR_COLORS = new int[]{0x00000000, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000};

    public final SqueakImageContext image;
    private final JFrame frame = new JFrame(DEFAULT_WINDOW_TITLE);
    private final Canvas canvas = new Canvas();
    private SqueakMouse mouse;
    private SqueakKeyboard keyboard;
    private final Deque<long[]> deferredEvents = new ArrayDeque<>();
    private final ScheduledExecutorService repaintExecutor;

    @CompilationFinal public boolean usesEventQueue = false;
    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons = 0;
    private Dimension windowSize = null;
    private boolean deferUpdates = false;

    public SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);

        // install event listeners
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        frame.addKeyListener(keyboard);

        frame.setTitle(SqueakDisplay.DEFAULT_WINDOW_TITLE + " (" + image.getImagePath() + ")");
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
        if (REPAINT_AUTOMATICALLY) {
            repaintExecutor = Executors.newSingleThreadScheduledExecutor();
            repaintExecutor.scheduleWithFixedDelay(new Runnable() {
                public void run() {
                    canvas.repaint();
                }
            }, 0, 20, TimeUnit.MILLISECONDS);
        } else {
            repaintExecutor = null;
        }
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
    public void close() {
        frame.setVisible(false);
        frame.dispose();
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
    public void open(final PointersObject sqDisplay) {
        canvas.setSqDisplay(sqDisplay);
        resizeTo(canvas.width, canvas.height);
        if (!frame.isVisible()) {
            frame.setVisible(true);
            frame.requestFocus();
        }
    }

    @Override
    public DisplayPoint getLastMousePosition() {
        final Point position = mouse.getPosition();
        return new DisplayPoint(position.getX(), position.getY());
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
    @TruffleBoundary
    public void setCursor(final int[] cursorWords, final int[] mask, final int depth) {
        final Dimension bestCursorSize = TOOLKIT.getBestCursorSize(SqueakIOConstants.CURSOR_WIDTH, SqueakIOConstants.CURSOR_HEIGHT);
        final Cursor cursor;
        if (bestCursorSize.width == 0 || bestCursorSize.height == 0) {
            cursor = Cursor.getDefaultCursor();
        } else {
            // TODO: Ensure the below works correctly for all cursor and maybe refactor senders.
            /**
             * <pre>
                  mask    cursor  effect
                  0        0     transparent (underlying pixel shows through)
                  1        1     opaque black
                  1        0     opaque white
                  0        1     invert the underlying pixel
             * </pre>
             */
            final int[] ints = mergeCursorWithMask(cursorWords, mask);
            final BufferedImage bufferedImage = new BufferedImage(SqueakIOConstants.CURSOR_WIDTH, SqueakIOConstants.CURSOR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < SqueakIOConstants.CURSOR_HEIGHT; y++) {
                final int word = ints[y];
                for (int x = 0; x < SqueakIOConstants.CURSOR_WIDTH; x++) {
                    final int colorIndex = (word >> ((SqueakIOConstants.CURSOR_WIDTH - 1 - x) * 2)) & 3;
                    bufferedImage.setRGB(x, y, CURSOR_COLORS[colorIndex]);
                }
            }
            cursor = TOOLKIT.createCustomCursor(bufferedImage, new Point(0, 0), "GraalSqueak Cursor");
        }
        canvas.setCursor(cursor);
    }

    @ExplodeLoop
    private static int[] mergeCursorWithMask(final int[] cursorWords, final int[] maskWords) {
        final int[] words = new int[SqueakIOConstants.CURSOR_HEIGHT];
        int cursorWord;
        int maskWord;
        int bit;
        int merged;
        for (int y = 0; y < SqueakIOConstants.CURSOR_HEIGHT; y++) {
            cursorWord = cursorWords[y];
            maskWord = maskWords[y];
            bit = 0x80000000;
            merged = 0;
            for (int x = 0; x < SqueakIOConstants.CURSOR_WIDTH; x++) {
                merged = merged | ((maskWord & bit) >> x) | ((cursorWord & bit) >> (x + 1));
                bit = bit >>> 1;
            }
            words[y] = merged;
        }
        return words;
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
        return SqueakIOConstants.NULL_EVENT.clone();
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        deferredEvents.add(new long[]{eventType, getEventTime(), value3, value4, value5, value6, 0L, 0L});
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

    private long getEventTime() {
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

    @Override
    public String getClipboardData() {
        String text;
        try {
            text = (String) getClipboard().getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            text = "";
        }
        return text;
    }

    @Override
    public void setClipboardData(final String text) {
        final StringSelection selection = new StringSelection(text);
        getClipboard().setContents(selection, selection);
    }

    @SuppressWarnings("static-method")
    private Clipboard getClipboard() {
        return Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    @Override
    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    public void pollEvents() {
        throw new SqueakException("No need to poll for events manually when using AWT.");
    }
}
