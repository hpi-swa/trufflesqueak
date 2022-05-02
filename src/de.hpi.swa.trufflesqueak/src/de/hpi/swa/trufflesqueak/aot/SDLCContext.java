/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.aot;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.c.CContext;

import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class SDLCContext implements CContext.Directives {
    @Override
    public boolean isInConfiguration() {
        /* SDL2 backend only supported on Linux and Darwin */
        return Platform.includedIn(Platform.LINUX.class) || Platform.includedIn(Platform.DARWIN.class);
    }

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList("<SDL2/SDL.h>");
    }

    @Override
    public List<String> getLibraries() {
        return Collections.singletonList("SDL2");
    }

    @Override
    public List<String> getOptions() {
        if (isInConfiguration()) {
            return Arrays.asList(SDL2Config.CFLAGS.split(" "));
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }

    private static class SDL2Config {
        private static final String CFLAGS;

        static {
            final List<String> lines = MiscUtils.exec("sdl2-config --cflags", 5);
            if (lines != null && lines.size() == 1) {
                CFLAGS = lines.get(0);
            } else {
                throw new UnsupportedOperationException("`sdl2-config --cflags` failed. Please make sure SDL2 is installed on your system.");
            }
        }
    }
}
