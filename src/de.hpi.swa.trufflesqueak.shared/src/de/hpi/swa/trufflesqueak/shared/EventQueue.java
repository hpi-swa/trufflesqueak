package de.hpi.swa.trufflesqueak.shared;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public final class EventQueue extends ConcurrentLinkedQueue<Runnable> {
    public static final EventQueue INSTANCE = new EventQueue();

    public static volatile Consumer<Object> osEventHandler = null;
}
