package de.hpi.swa.trufflesqueak.util;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Display extends BaseDisplay {
    private JFrame frame;
    private Canvas canvas;
// private BufferStrategy bufferStrategy;

    private Point mousePosition = new Point(0, 0);
    private int buttons = 0;

    public Display() {
        frame = new JFrame("TruffleSqueak");

        canvas = new Canvas();
        canvas.setBounds(0, 0, 400, 300);
        canvas.setIgnoreRepaint(true);
        canvas.createBufferStrategy(1);
        canvas.addMouseListener(new SqueakMouseListener());
        canvas.addMouseMotionListener(new SqueakMouseMotionListener(this));
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

    private class SqueakMouseListener implements MouseListener {

        public void mouseClicked(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mousePressed(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mouseReleased(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mouseEntered(MouseEvent e) {
            // TODO Auto-generated method stub

        }

        public void mouseExited(MouseEvent e) {
            // TODO Auto-generated method stub

        }
    }

    private class SqueakMouseMotionListener implements MouseMotionListener {
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
