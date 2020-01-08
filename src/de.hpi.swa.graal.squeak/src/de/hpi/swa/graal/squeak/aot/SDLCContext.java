/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.aot;

import java.util.Collections;
import java.util.List;

import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.c.CContext;

public class SDLCContext implements CContext.Directives {

    @Override
    public List<String> getHeaderFiles() {
        return Collections.singletonList("<SDL2/SDL.h>");
    }

    @Override
    public List<String> getLibraries() {
        if (Platform.includedIn(Platform.LINUX.class) || Platform.includedIn(Platform.DARWIN.class)) {
            // `sdl2-config --libs`
            return Collections.singletonList("-L/usr/local/lib -lSDL2");
        } else {
            throw new UnsupportedOperationException("Unsupported OS");
        }
    }
}
