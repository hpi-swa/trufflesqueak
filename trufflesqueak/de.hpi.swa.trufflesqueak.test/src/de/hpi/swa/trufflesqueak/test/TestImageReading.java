package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.util.Chunk;

public class TestImageReading extends TestSqueak {
    @Test
    public void testFloatDecoding() {
        Chunk chunk = new Chunk(
                        null,
                        image,
                        2, // 2 words
                        10, // float format, 32-bit words without padding word
                        34, // classid of BoxedFloat64
                        3833906, // identityHash for 1.0
                        0 // position
        );
        chunk.data().add(0);
        chunk.data().add(1072693248);
        assertEquals((double) chunk.asFloatObject(), 1.0);

        chunk.data().removeAllElements();
        chunk.data().add((int) 2482401462L);
        chunk.data().add(1065322751);
        assertEquals((double) chunk.asFloatObject(), 0.007699011184197404);

        chunk.data().removeAllElements();
        chunk.data().add(876402988);
        chunk.data().add(1075010976);
        assertEquals((double) chunk.asFloatObject(), 4.841431442464721);
    }
}
