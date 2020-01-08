/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.util;

/*
 * Represents not provided values to enable optional arguments in specializations.
 */
public final class NotProvided {
    public static final NotProvided SINGLETON = new NotProvided();

    private NotProvided() {
    }
}
