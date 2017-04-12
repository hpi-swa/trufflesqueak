package de.hpi.swa.trufflesqueak.test;

import junit.framework.TestCase;

import java.nio.ByteBuffer;

public class TestImageReading extends TestCase {
    static String simple_version_header_le() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(100);
        // byteBuffer.putInt
        return "";
    }

//    SIMPLE_VERSION_HEADER_LE=
//
//    pack("<i", 6502);
//    static SpurHeader = "    image_2 = (SIMPLE_VERSION_HEADER_LE  # 1
//               + pack("<i", header_size)  # 2 64 byte header
//               + pack("<i", 0)           # 3 no body
//               + pack("<i", 0)           # 4 old base addresss unset
//               + pack("<i", 0)           # 5 no spl objs array
//               + "\x12\x34\x56\x78"      # 6 last hash
//               + pack("<h", 480)         # 7 window 480 height
//               +     pack("<h", 640)     #   window 640 width
//               + pack("<i", 0)           # 8 not fullscreen
//               + pack("<i", 0)           # 9 no extra memory
//               + ("\x00" * (header_size - (9 * word_size))))
//";

    /*public void testSpurImageHeader
    {

    }*/
}
