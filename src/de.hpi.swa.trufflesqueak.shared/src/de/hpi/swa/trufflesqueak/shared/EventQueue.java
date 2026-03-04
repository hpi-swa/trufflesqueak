package de.hpi.swa.trufflesqueak.shared;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class EventQueue extends ConcurrentLinkedQueue<Runnable> {
    public static final EventQueue INSTANCE = new EventQueue();

}
