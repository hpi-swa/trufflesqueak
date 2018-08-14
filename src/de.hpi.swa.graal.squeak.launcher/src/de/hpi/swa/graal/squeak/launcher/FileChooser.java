package de.hpi.swa.graal.squeak.launcher;

import javax.swing.JFileChooser;

public final class FileChooser {
    public static String run() {
        final JFileChooser squeakImageChooser = new JFileChooser();
        final long result = squeakImageChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return squeakImageChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }
}
