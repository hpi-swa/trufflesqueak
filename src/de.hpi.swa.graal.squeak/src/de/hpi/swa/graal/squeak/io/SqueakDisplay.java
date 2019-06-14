package de.hpi.swa.graal.squeak.io;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
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
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.DRAG;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.WINDOW;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.plugins.DropPlugin;

public final class SqueakDisplay implements SqueakDisplayInterface {
    private static final String DEFAULT_WINDOW_TITLE = "GraalSqueak";
    private static final boolean REPAINT_AUTOMATICALLY = false; // For debugging purposes.
    private static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(200, 150);
    private static final Toolkit TOOLKIT = Toolkit.getDefaultToolkit();
    @CompilationFinal(dimensions = 1) private static final int[] CURSOR_COLORS = new int[]{0x00000000, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000};

    public final SqueakImageContext image;
    private final JFrame frame = new JFrame(DEFAULT_WINDOW_TITLE);
    private final Canvas canvas = new Canvas();
    private boolean hasVisibleHardwareCursor;
    private final SqueakMouse mouse;
    private final SqueakKeyboard keyboard;
    private final ArrayDeque<long[]> deferredEvents = new ArrayDeque<>();
    private final ScheduledExecutorService repaintExecutor;

    @CompilationFinal private int inputSemaphoreIndex = -1;

    public int buttons = 0;
    private Dimension rememberedWindowSize = null;
    private Point rememberedWindowLocation;
    private boolean deferUpdates = false;

    public SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        mouse = new SqueakMouse(this);
        keyboard = new SqueakKeyboard(this);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(MINIMUM_WINDOW_SIZE);
        frame.getContentPane().add(canvas);
        frame.setResizable(true);

        installEventListeners();
        if (REPAINT_AUTOMATICALLY) {
            repaintExecutor = Executors.newSingleThreadScheduledExecutor();
            repaintExecutor.scheduleWithFixedDelay(() -> canvas.repaint(), 0, 20, TimeUnit.MILLISECONDS);
        } else {
            repaintExecutor = null;
        }
    }

    @SuppressWarnings("unused")
    private void installEventListeners() {
        canvas.addMouseListener(mouse);
        canvas.addMouseMotionListener(mouse);
        canvas.addMouseWheelListener(mouse);
        new DropTarget(canvas, new SqueakDropTargetAdapter());
        frame.addKeyListener(keyboard);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(final WindowEvent e) {
                addWindowEvent(WINDOW.ACTIVATED);
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                addWindowEvent(WINDOW.CLOSE);
            }

            @Override
            public void windowIconified(final WindowEvent e) {
                addWindowEvent(WINDOW.ICONISE);
            }

            @Override
            public void windowStateChanged(final WindowEvent e) {
                addWindowEvent(WINDOW.METRIC_CHANGE);
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
            if (bitmap == null || bufferedImage == null) {
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
                    throw SqueakException.create("Unsupported form depth:",  depth);
            }
            //@formatter:on
            g.drawImage(bufferedImage, 0, 0, null);
        }

        private int[] decodeColors() {
            final int shift = 4 - depth;
            int pixelmask = (1 << depth) - 1 << shift;
            final int[] table = SqueakIOConstants.PIXEL_LOOKUP_TABLE[depth - 1];
            final int[] words = bitmap.getIntStorage();
            final int[] rgb = new int[words.length];
            for (int i = 0; i < words.length; i++) {
                final int pixel = (words[i] & pixelmask) >> shift - i * depth;
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
                x = i % width / 2 * 2;
                y = i / width;
                pixel = colorModel.getDataElements(high, pixel);
                raster.setDataElements(x, y, pixel);
                pixel = colorModel.getDataElements(low, pixel);
                raster.setDataElements(x + 1, y, pixel);
            }
            return raster;
        }

        private void setSqDisplay(final PointersObject sqDisplay) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            bitmap = (NativeObject) sqDisplay.at0(FORM.BITS);
            if (!bitmap.isIntType()) {
                throw SqueakException.create("Display bitmap expected to be a words object");
            }
            width = (int) (long) sqDisplay.at0(FORM.WIDTH);
            height = (int) (long) sqDisplay.at0(FORM.HEIGHT);
            depth = (int) (long) sqDisplay.at0(FORM.DEPTH);
            if (width > 0 && height > 0) {
                bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                repaint();
            }
        }
    }

    @Override
    @TruffleBoundary
    public void showDisplayBitsLeftTopRightBottom(final PointersObject destForm, final int left, final int top, final int right, final int bottom) {
        if (left < right && top < bottom && !deferUpdates && destForm.isDisplay()) {
            canvas.paintImmediately(left, top, right - left, bottom - top);
        }
    }

    @Override
    @TruffleBoundary
    public void showDisplayRect(final int left, final int right, final int top, final int bottom) {
        assert left < right && top < bottom;
        /**
         * {@link Canvas#repaint} informs the repaint manager that the canvas should soon be
         * repainted which is sufficient in most cases. When the user drags content and the hardware
         * cursor is invisible, however, it is necessary to {@link Canvas#paintImmediately} which is
         * expensive but avoids strange visual artifacts.
         */
        if (hasVisibleHardwareCursor) {
            canvas.repaint(0, left, top, right - left, bottom - top);
        } else {
            canvas.paintImmediately(left, top, right - left, bottom - top);
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
    public DisplayPoint getWindowSize() {
        return new DisplayPoint(frame.getContentPane().getWidth(), frame.getContentPane().getHeight());
    }

    @Override
    @TruffleBoundary
    public void setFullscreen(final boolean enable) {
        if (enable) {
            rememberedWindowLocation = frame.getLocationOnScreen();
            rememberedWindowSize = frame.getContentPane().getSize();
        }
        frame.dispose();
        frame.setUndecorated(enable);
        if (enable) {
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setResizable(false);
        } else {
            frame.setExtendedState(JFrame.NORMAL);
            frame.getContentPane().setPreferredSize(rememberedWindowSize);
            frame.setResizable(true);
        }
        frame.pack();
        frame.setLocation(rememberedWindowLocation);
        frame.setVisible(true);
    }

    @Override
    @TruffleBoundary
    public void open(final PointersObject sqDisplay) {
        canvas.setSqDisplay(sqDisplay);
        // Set or update frame title.
        frame.setTitle(SqueakDisplay.DEFAULT_WINDOW_TITLE + " (" + image.getImagePath() + ")");
        if (!frame.isVisible()) {
            final DisplayPoint lastWindowSize = image.flags.getLastWindowSize();
            frame.getContentPane().setPreferredSize(new Dimension(lastWindowSize.getWidth(), lastWindowSize.getHeight()));
            frame.pack();
            frame.setVisible(true);
            frame.requestFocus();
        }
    }

    @Override
    @TruffleBoundary
    public boolean isVisible() {
        return frame.isVisible();
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
            boolean allZero = true;
            for (final int cursorWord : cursorWords) {
                if (cursorWord != 0) {
                    allZero = false;
                    break;
                }
            }
            hasVisibleHardwareCursor = !allZero;
            final int[] ints;
            if (mask != null) {
                ints = mergeCursorWithMask(cursorWords, mask);
            } else {
                ints = cursorWords;
            }
            final BufferedImage bufferedImage = new BufferedImage(SqueakIOConstants.CURSOR_WIDTH, SqueakIOConstants.CURSOR_HEIGHT, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < SqueakIOConstants.CURSOR_HEIGHT; y++) {
                final int word = ints[y];
                for (int x = 0; x < SqueakIOConstants.CURSOR_WIDTH; x++) {
                    final int colorIndex = word >> (SqueakIOConstants.CURSOR_WIDTH - 1 - x) * 2 & 3;
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
                merged = merged | (maskWord & bit) >> x | (cursorWord & bit) >> x + 1;
                bit = bit >>> 1;
            }
            words[y] = merged;
        }
        return words;
    }

    @Override
    public long[] getNextEvent() {
        final long[] nextEvent = deferredEvents.pollFirst();
        if (nextEvent == null) {
            return SqueakIOConstants.newNullEvent();
        } else {
            return nextEvent;
        }
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6) {
        addEvent(eventType, value3, value4, value5, value6, 0L, 0L);
    }

    private void addDragEvent(final long type, final Point location) {
        addEvent(EVENT_TYPE.DRAG_DROP_FILES, type, (long) location.getX(), (long) location.getY(), buttons >> 3, DropPlugin.getFileListSize(image), 0L);
    }

    private void addWindowEvent(final long type) {
        addEvent(EVENT_TYPE.WINDOW, type, 0L, 0L, 0L);
    }

    public void addEvent(final long eventType, final long value3, final long value4, final long value5, final long value6, final long value7, final long value8) {
        deferredEvents.add(new long[]{eventType, getEventTime(), value3, value4, value5, value6, value7, value8});
        if (inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }

    public int recordModifiers(final InputEvent e) {
        final int shiftValue = e.isShiftDown() ? KEYBOARD.SHIFT : 0;
        final int ctrlValue = e.isControlDown() && !e.isAltDown() ? KEYBOARD.CTRL : 0;
        final int cmdValue = e.isMetaDown() || e.isAltDown() && !e.isControlDown() ? KEYBOARD.CMD : 0;
        final int modifiers = shiftValue + ctrlValue + cmdValue;
        buttons = buttons & ~KEYBOARD.ALL | modifiers;
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
    public boolean getDeferUpdates() {
        return deferUpdates;
    }

    @Override
    @TruffleBoundary
    public void setWindowTitle(final String title) {
        frame.setTitle(title);
    }

    @Override
    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @Override
    public String getClipboardData() {
        try {
            return (String) getClipboard().getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException e) {
            return "";
        }
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
    @TruffleBoundary
    public void beep() {
        Toolkit.getDefaultToolkit().beep();
    }

    @Override
    public void pollEvents() {
        throw SqueakException.create("No need to poll for events manually when using AWT.");
    }

    private final class SqueakDropTargetAdapter extends DropTargetAdapter {

        @Override
        public void drop(final DropTargetDropEvent dtde) {
            final Transferable transferable = dtde.getTransferable();
            for (final DataFlavor flavor : transferable.getTransferDataFlavors()) {
                if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    try {
                        @SuppressWarnings("unchecked")
                        final List<File> l = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        final Iterator<File> iter = l.iterator();
                        final String[] fileList = new String[l.size()];
                        int i = 0;
                        while (iter.hasNext()) {
                            fileList[i++] = iter.next().getCanonicalPath();
                        }
                        DropPlugin.updateFileList(image, fileList);
                        addDragEvent(DRAG.DROP, dtde.getLocation());
                        dtde.getDropTargetContext().dropComplete(true);
                        return;
                    } catch (final UnsupportedFlavorException e) {
                        CompilerDirectives.transferToInterpreter();
                        e.printStackTrace();
                    } catch (final IOException e) {
                        CompilerDirectives.transferToInterpreter();
                        e.printStackTrace();
                    }
                }
            }
            dtde.rejectDrop();
        }

        @Override
        public void dragEnter(final DropTargetDragEvent e) {
            addDragEvent(DRAG.ENTER, e.getLocation());
        }

        @Override
        public void dragExit(final DropTargetEvent e) {
            addDragEvent(DRAG.LEAVE, new Point(0, 0));
        }

        @Override
        public void dragOver(final DropTargetDragEvent e) {
            addDragEvent(DRAG.MOVE, e.getLocation());
        }
    }
}
