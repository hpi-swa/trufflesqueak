/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared.sdl;

public class SDLInit {

    public static final int
            SDL_INIT_AUDIO    = 0x00000010,
            SDL_INIT_VIDEO    = 0x00000020,
            SDL_INIT_JOYSTICK = 0x00000200,
            SDL_INIT_HAPTIC   = 0x00001000,
            SDL_INIT_GAMEPAD  = 0x00002000,
            SDL_INIT_EVENTS   = 0x00004000,
            SDL_INIT_SENSOR   = 0x00008000,
            SDL_INIT_CAMERA   = 0x00010000;

    public static final int
            SDL_APP_CONTINUE = 0,
            SDL_APP_SUCCESS  = 1,
            SDL_APP_FAILURE  = 2;

    public static final String
            SDL_PROP_APP_METADATA_NAME_STRING       = "SDL.app.metadata.name",
            SDL_PROP_APP_METADATA_VERSION_STRING    = "SDL.app.metadata.version",
            SDL_PROP_APP_METADATA_IDENTIFIER_STRING = "SDL.app.metadata.identifier",
            SDL_PROP_APP_METADATA_CREATOR_STRING    = "SDL.app.metadata.creator",
            SDL_PROP_APP_METADATA_COPYRIGHT_STRING  = "SDL.app.metadata.copyright",
            SDL_PROP_APP_METADATA_URL_STRING        = "SDL.app.metadata.url",
            SDL_PROP_APP_METADATA_TYPE_STRING       = "SDL.app.metadata.type";

    protected SDLInit() {
        throw new UnsupportedOperationException();
    }
}
