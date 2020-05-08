/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.launcher;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

public final class FileChooser {
    public static String run() {
        final JFileChooser squeakImageChooser = new JFileChooser();
        squeakImageChooser.setFileFilter(new SqueakImageFilter());
        squeakImageChooser.setApproveButtonToolTipText("Open selected image with GraalSqueak");
        final long result = squeakImageChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return squeakImageChooser.getSelectedFile().getAbsolutePath();
        }
        return null;
    }

    private static class SqueakImageFilter extends FileFilter {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".image");
        }

        @Override
        public String getDescription() {
            return "Squeak/Smalltalk images";
        }
    }
}
