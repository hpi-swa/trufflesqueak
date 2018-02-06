package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class SqueakImageReadingTest extends AbstractSqueakTestCase {
    @Test
    public void testFloatDecoding() {
        SqueakImageChunk chunk = new SqueakImageChunk(
                        null,
                        image,
                        2, // 2 words
                        10, // float format, 32-bit words without padding word
                        34, // classid of BoxedFloat64
                        3833906, // identityHash for 1.0
                        0 // position
        );
        chunk.data().add(0L);
        chunk.data().add(1072693248L);
        assertEquals((double) chunk.asFloatObject(), 1.0);

        chunk.data().removeAllElements();
        chunk.data().add(2482401462L);
        chunk.data().add(1065322751L);
        assertEquals((double) chunk.asFloatObject(), 0.007699011184197404);

        chunk.data().removeAllElements();
        chunk.data().add(876402988L);
        chunk.data().add(1075010976L);
        assertEquals((double) chunk.asFloatObject(), 4.841431442464721);
    }
}
