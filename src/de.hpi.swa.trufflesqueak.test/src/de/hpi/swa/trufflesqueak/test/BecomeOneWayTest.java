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
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{b, a}, new Object[]{c, b}, false);
        assertTrue(image.objectGraphUtils.isUnfollowNeeded());
        assertSame(c, a.resolveForwardingPointer());
        assertSame(c, b.resolveForwardingPointer());
        assertTrue(c.isNotForwarded());

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

        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a}, new Object[]{b}, false);
        assertSame(b, a.resolveForwardingPointer());

        // Extending B -> C must collapse A's forwarding pointer directly to C.
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{b}, new Object[]{c}, false);
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
    public void testForwardedObjectsInArrays() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();

        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a}, new Object[]{b}, false);

        // Heap slots may still refer to the forwarded A. As `to`, it stands for B (B -> B).
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{b}, new Object[]{a}, false);
        assertSame(b, a.resolveForwardingPointer());
        assertTrue(b.isNotForwarded());

        // As `from`, it stands for B as well (B -> C).
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a}, new Object[]{c}, false);
        assertSame(c, a.resolveForwardingPointer());
        assertSame(c, b.resolveForwardingPointer());
        assertTrue(c.isNotForwarded());

        image.objectGraphUtils.unfollow();

        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b arr |
                        a := Object new.
                        b := Object new.
                        arr := { a }.
                        { a } elementsForwardIdentityTo: { b }.
                        { b } elementsForwardIdentityTo: arr.
                        (arr first == b) and: [ a == b ]
                        """));
        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c arr ref |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        arr := { a }.
                        ref := { a }.
                        { a } elementsForwardIdentityTo: { b }.
                        arr elementsForwardIdentityTo: { c }.
                        (ref first == c) and: [ b == c ]
                        """));
    }

    @Test
    public void testCyclesAreProcessedInOrder() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash d = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash e = newDummyObject();

        // A -> B, then B -> A stands for B -> B.
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, b}, new Object[]{b, a}, true);
        assertSame(b, a.resolveForwardingPointer());
        assertTrue(b.isNotForwarded());

        // C -> D, D -> E, then E -> D stands for E -> E.
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{c, d, e}, new Object[]{d, e, d}, true);
        assertSame(e, c.resolveForwardingPointer());
        assertSame(e, d.resolveForwardingPointer());
        assertTrue(e.isNotForwarded());

        image.objectGraphUtils.unfollow();

        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b refA refB |
                        a := Object new.
                        b := Object new.
                        refA := { a }.
                        refB := { b }.
                        { a. b } elementsForwardIdentityTo: { b. a }.
                        (refA first == b) and: [ refB first == b ]
                        """));
    }

    @Test
    public void testDuplicateFromEntries() {
        final AbstractSqueakObjectWithClassAndHash a = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash b = newDummyObject();
        final AbstractSqueakObjectWithClassAndHash c = newDummyObject();
        final long originalHashC = c.getOrCreateSqueakHash();

        // The first occurrence wins, the second one is ignored.
        image.objectGraphUtils.pointersBecomeOneWay(new Object[]{a, a}, new Object[]{b, c}, true);
        assertSame(b, a.resolveForwardingPointer());
        assertTrue(b.isNotForwarded());
        assertTrue(c.isNotForwarded());
        assertEquals(originalHashC, c.getOrCreateSqueakHash());

        image.objectGraphUtils.unfollow();

        assertEquals(BooleanObject.TRUE, evaluate("""
                        | a b c refA |
                        a := Object new.
                        b := Object new.
                        c := Object new.
                        refA := { a }.
                        { a. a } elementsForwardIdentityTo: { b. c }.
                        (refA first == b) and: [ b ~~ c ]
                        """));
    }
}
