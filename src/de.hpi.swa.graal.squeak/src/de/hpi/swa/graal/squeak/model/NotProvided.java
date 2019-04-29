package de.hpi.swa.graal.squeak.model;

/*
 * Represents not provided values to enable optional arguments in specializations.
 */
public final class NotProvided {
    public static final NotProvided SINGLETON = new NotProvided();

    private NotProvided() {
    }
}
