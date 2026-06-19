/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.graalvm.home.HomeFinder;

public final class SqueakImageLocator {
    /* Ensures that TruffleSqueak's resources directory exists and returns path to image file. */
    public static String findImage(final String userImage) {
        return findImage(userImage, null, false);
    }

    public static String findImage(final String userImage, final String imageKey, final boolean isQuiet) {
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
        if (imageFile != null && imageKey == null) {
            return imageFile;
        } else {
            final SqueakLanguageConfig.SupportedImage[] supportedImages = SqueakLanguageConfig.SUPPORTED_IMAGES;
            final PrintStream out = System.out; // ignore checkstyle
            final SqueakLanguageConfig.SupportedImage selectedEntry;
            if (imageKey != null) {
                selectedEntry = isDirectDownloadUrl(imageKey) ? SqueakLanguageConfig.SupportedImage.url(imageKey) : findSupportedImage(supportedImages, imageKey);
            } else if (isQuiet) {
                selectedEntry = supportedImages[0];
            } else {
                selectedEntry = supportedImages[askUserToChooseImage(supportedImages, out)];
            }
            if (!isQuiet) {
                out.printf("Downloading %s...%n", selectedEntry.name());
            }
            final String downloadedImage = downloadAndUnzip(selectedEntry.url(), resourcesDirectory);
            return downloadedImage != null ? downloadedImage : Objects.requireNonNull(findImageFile(resourcesDirectory));
        }
    }

    private static int askUserToChooseImage(final SqueakLanguageConfig.SupportedImage[] supportedImages, final PrintStream out) {
        int selection;
        final Scanner userInput = new Scanner(System.in);
        for (int i = 0; i < supportedImages.length; i++) {
            out.printf("%s) %s%n", i + 1, supportedImages[i].name());
        }
        out.print("Choose Smalltalk image: ");
        selection = -1;
        try {
            selection = userInput.nextInt() - 1;
        } catch (final NoSuchElementException e) {
            // ignore
        }
        if (!(0 <= selection && selection < supportedImages.length)) {
            throw new RuntimeException("Invalid selection. Please try again.");
        }
        return selection;
    }

    static SqueakLanguageConfig.SupportedImage findSupportedImage(final SqueakLanguageConfig.SupportedImage[] supportedImages, final String imageKey) {
        for (final SqueakLanguageConfig.SupportedImage supportedImage : supportedImages) {
            if (supportedImage.id().equals(imageKey)) {
                return supportedImage;
            }
        }
        final StringBuilder availableKeys = new StringBuilder();
        for (int i = 0; i < supportedImages.length; i++) {
            if (i > 0) {
                availableKeys.append(", ");
            }
            availableKeys.append(supportedImages[i].id());
        }
        throw new RuntimeException("Unknown image key '" + imageKey + "'. Available keys: " + availableKeys);
    }

    static boolean isDirectDownloadUrl(final String imageKey) {
        return imageKey.startsWith("https://");
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

    private static String downloadAndUnzip(final String url, final File destDirectory) {
        try (BufferedInputStream bis = ImageDownloadSupport.openStream(URI.create(url))) {
            return unzip(bis, destDirectory);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String unzip(final BufferedInputStream bis, final File destDirectory) throws IOException {
        final ZipInputStream zis = new ZipInputStream(bis);
        ZipEntry zipEntry = zis.getNextEntry();
        String extractedImage = null;
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
                try (OutputStream fos = Files.newOutputStream(destFile.toPath())) {
                    zis.transferTo(fos);
                }
                if (zipEntry.getName().endsWith(".image")) {
                    extractedImage = destFile.toPath().toString();
                }
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
        return extractedImage;
    }

    private static void ensureDirectory(final File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Failed to create directory " + directory);
        }
    }
}
