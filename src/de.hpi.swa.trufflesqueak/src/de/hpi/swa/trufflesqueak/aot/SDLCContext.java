/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.aot;

import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.c.CContext;

public final class SDLCContext implements CContext.Directives {
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
        /* `sdl2-config --cflags` */
        if (Platform.includedIn(Platform.LINUX.class)) {
            return Collections.singletonList("-I/usr/include/SDL2 -D_REENTRANT");
        } else if (Platform.includedIn(Platform.DARWIN.class)) {
            return Collections.singletonList("-I/usr/local/include/SDL2 -D_THREAD_SAFE");
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }
}
