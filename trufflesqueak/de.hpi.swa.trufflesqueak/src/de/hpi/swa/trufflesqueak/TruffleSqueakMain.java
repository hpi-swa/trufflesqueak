package de.hpi.swa.trufflesqueak;

import java.io.IOException;

import javax.swing.JFileChooser;

public class TruffleSqueakMain {
    private static void run(String filename) {
        try {
            SqueakImage image = new ImageReader(filename).readImage();
            image.run();
        } catch (IOException e) {
            System.out.println("Error reading " + filename + ". " + e.getMessage());
            return;
        }
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            run(args[0]);
        } else {
            JFileChooser squeakImageChooser = new JFileChooser();
            int result = squeakImageChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                run(squeakImageChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }
}
