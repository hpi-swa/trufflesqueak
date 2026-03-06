package de.hpi.swa.trufflesqueak.shared;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.lwjgl.sdl.SDL_Event;

public final class EventQueue extends ConcurrentLinkedQueue<Runnable> {
    public static final EventQueue INSTANCE = new EventQueue();

    public static volatile Consumer<SDL_Event> osEventHandler = null;
}
