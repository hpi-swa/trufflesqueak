package de.hpi.swa.trufflesqueak.util;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;

public final class Display {

    public static AbstractDisplay create(boolean noDisplay) {
        if (!GraphicsEnvironment.isHeadless() && !noDisplay) {
            return new JavaDisplay();
        } else {
            return new NullDisplay();
        }
    }

    private static final class MOUSE_BUTTON {
        public static final int ALL = 1 + 2 + 4;
        public static final int BLUE = 1;
        public static final int YELLOW = 2;
        public static final int RED = 4;
    }

    private static final class KEYBOARD_MODIFIER {
        public static final int ALL = 8 + 16 + 32 + 64;
        public static final int SHIFT = 8;
        public static final int CTRL = 16;
        public static final int ALT = 32;
        public static final int CMD = 64;
    }

    public static abstract class AbstractDisplay {
        public abstract void drawRect(int left, int right, int top, int bottom);

        public abstract Dimension getSize();

        public abstract int getButtons();

        public abstract Point getMousePosition();

        public abstract void setFullscreen(boolean enable);

        public abstract void forceUpdate();

        public abstract int nextKey();

        public abstract int peekKey();

        public abstract void open();

        public abstract void close();
    }

    public static class JavaDisplay extends AbstractDisplay {
        private JFrame frame = new JFrame("TruffleSqueak");
        private Canvas canvas = new Canvas();
        // private BufferStrategy bufferStrategy;

        private Point mousePosition = new Point(0, 0);
        private int buttons = 0;
        public int modifiers;
        public Deque<Integer> keys = new ArrayDeque<>();

        public JavaDisplay() {
            canvas.setBounds(0, 0, 800, 600);
            canvas.setIgnoreRepaint(true);
            canvas.createBufferStrategy(1);
            canvas.addMouseListener(new SqueakMouseListener(this));
            canvas.addMouseMotionListener(new SqueakMouseMotionListener(this));
            canvas.addKeyListener(new SqueakKeyListener(this));
            // bufferStrategy = canvas.getBufferStrategy();

            JPanel panel = (JPanel) frame.getContentPane();
            panel.setPreferredSize(new Dimension(800, 600));
            panel.setLayout(null);
            panel.add(canvas);

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack();
            frame.setResizable(true);
        }

        private static class SqueakMouseListener implements MouseListener {
            private JavaDisplay display;

            public SqueakMouseListener(JavaDisplay display) {
                this.display = display;
            }

            public void mouseClicked(MouseEvent e) {
                // TODO Auto-generated method stub

            }

            public void mousePressed(MouseEvent e) {
                int buttons = display.buttons & MOUSE_BUTTON.ALL;
                switch (e.getButton()) {
                    case 0: // left
                        buttons = MOUSE_BUTTON.RED;
                        break;
                    case 1: // middle
                        buttons = MOUSE_BUTTON.YELLOW;
                        break;
                    case 2: // right
                        buttons = MOUSE_BUTTON.BLUE;
                        break;
                }
                display.updateButtons(buttons);
            }

            public void mouseReleased(MouseEvent e) {
                display.buttons = 0;

            }

            public void mouseEntered(MouseEvent e) {
                // TODO Auto-generated method stub
            }

            public void mouseExited(MouseEvent e) {
                // TODO Auto-generated method stub
            }
        }

        private static class SqueakMouseMotionListener implements MouseMotionListener {
            private JavaDisplay display;

            public SqueakMouseMotionListener(JavaDisplay display) {
                this.display = display;
            }

            public void mouseDragged(MouseEvent e) {
                // TODO Auto-generated method stub
            }

            public void mouseMoved(MouseEvent e) {
                display.mousePosition = e.getPoint();
            }
        }

        private static class SqueakKeyListener implements KeyListener {
            private JavaDisplay display;

            public SqueakKeyListener(JavaDisplay display) {
                this.display = display;
            }

            public void keyTyped(KeyEvent e) {
                display.keys.add(e.getKeyCode());
            }

            public void keyPressed(KeyEvent e) {
                boolean shiftPressed = e.isShiftDown();
                boolean ctrlPressed = e.isControlDown() && !e.isAltDown();
                boolean cmdPressed = e.isMetaDown() || (e.isAltDown() && !e.isControlDown());
                int modifiers = (shiftPressed ? KEYBOARD_MODIFIER.SHIFT : 0) +
                                (ctrlPressed ? KEYBOARD_MODIFIER.CTRL : 0) +
                                (cmdPressed ? KEYBOARD_MODIFIER.CMD : 0);
                display.buttons = (display.buttons & ~KEYBOARD_MODIFIER.ALL) | modifiers;
            }

            public void keyReleased(KeyEvent e) {
                display.buttons = (display.buttons & ~KEYBOARD_MODIFIER.ALL);
            }
        }

        private void updateButtons(int newButtons) {
            buttons = newButtons | modifiers;
        }

        @Override
        public void drawRect(int left, int right, int top, int bottom) {
            // Graphics g = bufferStrategy.getDrawGraphics();
            // TODO: implement drawRect
            throw new SqueakException("Not yet implemented");
        }

        @Override
        public void forceUpdate() {
            // TODO: implement force update
            throw new SqueakException("Not yet implemented");
        }

        @Override
        public void open() {
            if (!frame.isVisible()) {
                frame.setVisible(true);
                canvas.requestFocus();
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
        public int getButtons() {
            return buttons;
        }

        @Override
        public Point getMousePosition() {
            return mousePosition;
        }

        @Override
        public void setFullscreen(boolean enable) {
            if (enable) {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setUndecorated(true);
            } else {
                frame.setExtendedState(JFrame.NORMAL);
                frame.setUndecorated(false);
            }
        }

        @Override
        public int nextKey() {
            return keys.pop();
        }

        @Override
        public int peekKey() {
            return keys.peek();
        }
    }

    public static class NullDisplay extends AbstractDisplay {
        @Override
        public void drawRect(int left, int right, int top, int bottom) {
        }

        @Override
        public Dimension getSize() {
            return new Dimension(0, 0);
        }

        @Override
        public int getButtons() {
            return 0;
        }

        @Override
        public Point getMousePosition() {
            return new Point(0, 0);
        }

        @Override
        public void setFullscreen(boolean enable) {
        }

        @Override
        public void forceUpdate() {
        }

        @Override
        public int nextKey() {
            return 0;
        }

        @Override
        public int peekKey() {
            return 0;
        }

        @Override
        public void open() {
        }

        @Override
        public void close() {
        }
    }
}
