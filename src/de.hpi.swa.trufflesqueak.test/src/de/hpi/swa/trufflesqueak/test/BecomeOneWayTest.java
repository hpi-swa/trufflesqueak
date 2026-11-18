/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NilObject;

@SuppressWarnings("static-method")
public final class BecomeOneWayTest extends AbstractSqueakTestCaseWithImage {

    private static AbstractSqueakObjectWithClassAndHash newDummyObject() {
        return image.asArrayOfObjects(NilObject.SINGLETON);
    }

    @After
    public void cleanUpBecomeMap() {
        if (image.objectGraphUtils.isUnfollowNeeded()) {
            image.objectGraphUtils.unfollow();
        }
    }

    @Test
    public void testBulkReverseOrderedChain() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();

        // B -> C appears before A -> B; both must resolve directly to C in one hop.
        assertTrue(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{b, a}, new Object[]{c, b}, false));
        assertTrue(image.objectGraphUtils.isUnfollowNeeded());
        assertSame(c, a.resolveForwardingPointer());
        assertSame(c, b.resolveForwardingPointer());
        assertTrue(c.assertNotForwarded());

        image.objectGraphUtils.unfollow();
        assertFalse(image.objectGraphUtils.isUnfollowNeeded());

        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c refA refB |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        refA := { a }.
                        refB := { b }.
                        { b. a } elementsForwardIdentityTo: { c. b }.
                        (refA first == c) and: [ refB first == c ]
                        """));
    }

    @Test
    public void testConsecutiveLazyChains() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();

        assertTrue(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a}, new Object[]{b}, false));
        assertSame(b, a.resolveForwardingPointer());

        // Extending B -> C must collapse A's forwarding pointer directly to C.
        assertTrue(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{b}, new Object[]{c}, false));
        assertSame(c, a.resolveForwardingPointer());
        assertSame(c, b.resolveForwardingPointer());

        image.objectGraphUtils.unfollow();

        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c refA |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        refA := { a }.
                        { a } elementsForwardIdentityTo: { b }.
                        { b } elementsForwardIdentityTo: { c }.
                        refA first == c
                        """));
    }

    @Test
    public void testLazyChainFollowedByEagerImmediateBecome() {
        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b refA refB |
                        a := Object new.
                        b := Object new.
                        refA := { a }.
                        refB := { b }.
                        { a } elementsForwardIdentityTo: { b }.
                        { b } elementsForwardIdentityTo: { nil }.
                        (refA first == nil) and: [ refB first == nil ]
                        """));
        assertFalse(image.objectGraphUtils.isUnfollowNeeded());
    }

    @Test
    public void testRejectsCyclesAndDuplicateFromEntries() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();
        final long originalHashB = b.getOrCreateSqueakHash();

        // Direct two-element cycle (A -> B, B -> A)
        assertFalse(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, b}, new Object[]{b, a}, true));
        // Non-starting cycle (A -> B -> C -> B)
        assertFalse(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, b, c}, new Object[]{b, c, b}, true));
        // Duplicate `from` entries
        assertFalse(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, a}, new Object[]{b, c}, true));
        assertFalse(image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, a}, new Object[]{a, b}, true));

        // Failed calls must not mutate becomeMap, forwarding pointers, or target hashes.
        assertFalse(image.objectGraphUtils.isUnfollowNeeded());
        assertTrue(a.assertNotForwarded());
        assertTrue(b.assertNotForwarded());
        assertTrue(c.assertNotForwarded());
        assertEquals(originalHashB, b.getOrCreateSqueakHash());
    }

    @Test
    public void testPrimitiveRejectsCyclesAndDuplicates() {
        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        [ { a. b. c } elementsForwardIdentityTo: { b. c. b }. false ]
                            on: Error
                            do: [ :ex | true ]
                        """));
        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        [ { a. a } elementsForwardIdentityTo: { b. c }. false ]
                            on: Error
                            do: [ :ex | true ]
                        """));
    }
}
