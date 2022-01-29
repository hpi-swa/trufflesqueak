/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.graalvm.home.HomeFinder;

public final class SqueakImageLocator {
    private static final String[][] SMALLTALK_IMAGES = {
                    new String[]{"TruffleSqueak image (21.3.0) (recommended)", "https://github.com/hpi-swa/trufflesqueak/releases/download/21.3.0/TruffleSqueakImage-21.3.0.zip"},
                    new String[]{"TruffleSqueak test image (6.0alpha-20228b)", "https://github.com/hpi-swa/trufflesqueak/releases/download/21.1.0/TruffleSqueakTestImage-6.0alpha-20228b-64bit.zip"},
                    new String[]{"Cuis-Smalltalk test image (6.0-5053)", "https://github.com/hpi-swa/trufflesqueak/releases/download/21.3.0/CuisTestImage-6.0-5053.zip"}};

    /* Ensures that TruffleSqueak's resources directory exists and returns path to image file. */
    public static String findImage(final String userImage) {
        final File resourcesDirectory = findResourcesDirectory();
        try {
            ensureDirectory(resourcesDirectory);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        if (userImage != null) {
            return userImage;
        }
        final String imageFile = findImageFile(resourcesDirectory);
        if (imageFile != null) {
            return imageFile;
        } else {
            final Scanner userInput = new Scanner(System.in);
            final PrintStream out = System.out; // ignore checkstyle
            for (int i = 0; i < SMALLTALK_IMAGES.length; i++) {
                out.println(String.format("%d) %s", i + 1, SMALLTALK_IMAGES[i][0]));
            }
            out.print("Choose Smalltalk image: ");
            int selection = -1;
            try {
                selection = userInput.nextInt() - 1;
            } catch (final NoSuchElementException e) {
                // ignore
            }
            if (!(0 <= selection && selection < SMALLTALK_IMAGES.length)) {
                throw new RuntimeException("Invalid selection. Please try again.");
            }
            final String[] selectedEntry = SMALLTALK_IMAGES[selection];
            out.println(String.format("Downloading %s...", selectedEntry[0]));

            downloadAndUnzip(selectedEntry[1], resourcesDirectory);
            return Objects.requireNonNull(findImageFile(resourcesDirectory));
        }
    }

    private static String findImageFile(final File resourcesDirectory) {
        final String[] imageFiles = resourcesDirectory.list((dir, name) -> dir.equals(resourcesDirectory) && name.endsWith(".image"));
        if (imageFiles != null && imageFiles.length > 0) {
            /* Sort imageFiles alphabetically and return the last. */
            Arrays.sort(imageFiles);
            return resourcesDirectory.toPath().resolve(imageFiles[imageFiles.length - 1]).toString();
        } else {
            return null;
        }
    }

    private static File findResourcesDirectory() {
        final Path languageHome = HomeFinder.getInstance().getLanguageHomes().get(SqueakLanguageConfig.ID);
        if (languageHome == null) {
            throw new RuntimeException("Unable to locate TruffleSqueak's language home.");
        }
        return languageHome.resolve("resources").toFile();
    }

    private static void downloadAndUnzip(final String url, final File destDirectory) {
        try {
            final BufferedInputStream bis = new BufferedInputStream(new URL(url).openStream());
            unzip(bis, destDirectory);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void unzip(final BufferedInputStream bis, final File destDirectory) throws IOException {
        final byte[] buffer = new byte[2048];
        final ZipInputStream zis = new ZipInputStream(bis);
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            final File destFile = new File(destDirectory, zipEntry.getName());
            // https://snyk.io/research/zip-slip-vulnerability
            if (!destFile.getCanonicalPath().startsWith(destDirectory.getCanonicalPath() + File.separator)) {
                throw new IOException("Zip entry is outside of the dest dir: " + zipEntry.getName());
            }
            if (zipEntry.isDirectory()) {
                ensureDirectory(destFile);
            } else {
                ensureDirectory(destFile.getParentFile());
                final FileOutputStream fos = new FileOutputStream(destFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    private static void ensureDirectory(final File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory " + directory);
        }
    }
}
