package de.hpi.swa.trufflesqueak.util;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.hpi.swa.trufflesqueak.util.Constants.KEYBOARD_MODIFIER;
import de.hpi.swa.trufflesqueak.util.Constants.MOUSE_BUTTON;

public class Display extends BaseDisplay {
    private JFrame frame;
    private Canvas canvas;
// private BufferStrategy bufferStrategy;

    private Point mousePosition = new Point(0, 0);
    private int buttons = 0;
    public int modifiers;

    public Display() {
        frame = new JFrame("TruffleSqueak");

        canvas = new Canvas();
        canvas.setBounds(0, 0, 400, 300);
        canvas.setIgnoreRepaint(true);
        canvas.createBufferStrategy(1);
        canvas.addMouseListener(new SqueakMouseListener(this));
        canvas.addMouseMotionListener(new SqueakMouseMotionListener(this));
        canvas.addKeyListener(new SqueakKeyListener(this));
// bufferStrategy = canvas.getBufferStrategy();

        JPanel panel = (JPanel) frame.getContentPane();
        panel.setPreferredSize(new Dimension(400, 300));
        panel.setLayout(null);
        panel.add(canvas);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setResizable(true);
        frame.setVisible(true);

        canvas.requestFocus();
    }

    private static class SqueakMouseListener implements MouseListener {
        private Display display;

        public SqueakMouseListener(Display display) {
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
        private Display display;

        public SqueakMouseMotionListener(Display display) {
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
        private Display display;

        public SqueakKeyListener(Display display) {
            this.display = display;
        }

        public void keyTyped(KeyEvent e) {
            // TODO Auto-generated method stub
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
}
