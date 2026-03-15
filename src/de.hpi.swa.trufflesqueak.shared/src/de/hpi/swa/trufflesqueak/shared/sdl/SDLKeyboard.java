/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared.sdl;

public class SDLKeyboard {

    public static final int SDL_TEXTINPUT_TYPE_TEXT = 0,
                    SDL_TEXTINPUT_TYPE_TEXT_NAME = 1,
                    SDL_TEXTINPUT_TYPE_TEXT_EMAIL = 2,
                    SDL_TEXTINPUT_TYPE_TEXT_USERNAME = 3,
                    SDL_TEXTINPUT_TYPE_TEXT_PASSWORD_HIDDEN = 4,
                    SDL_TEXTINPUT_TYPE_TEXT_PASSWORD_VISIBLE = 5,
                    SDL_TEXTINPUT_TYPE_NUMBER = 6,
                    SDL_TEXTINPUT_TYPE_NUMBER_PASSWORD_HIDDEN = 7,
                    SDL_TEXTINPUT_TYPE_NUMBER_PASSWORD_VISIBLE = 8;

    public static final int SDL_CAPITALIZE_NONE = 0,
                    SDL_CAPITALIZE_SENTENCES = 1,
                    SDL_CAPITALIZE_WORDS = 2,
                    SDL_CAPITALIZE_LETTERS = 3;

    public static final String SDL_PROP_TEXTINPUT_TYPE_NUMBER = "SDL.textinput.type",
                    SDL_PROP_TEXTINPUT_CAPITALIZATION_NUMBER = "SDL.textinput.capitalization",
                    SDL_PROP_TEXTINPUT_AUTOCORRECT_BOOLEAN = "SDL.textinput.autocorrect",
                    SDL_PROP_TEXTINPUT_MULTILINE_BOOLEAN = "SDL.textinput.multiline",
                    SDL_PROP_TEXTINPUT_ANDROID_INPUTTYPE_NUMBER = "SDL.textinput.android.inputtype";

    protected SDLKeyboard() {
        throw new UnsupportedOperationException();
    }
}
