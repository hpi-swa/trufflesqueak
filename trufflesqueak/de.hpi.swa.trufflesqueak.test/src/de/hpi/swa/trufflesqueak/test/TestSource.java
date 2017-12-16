package de.hpi.swa.trufflesqueak.test;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.instrumentation.CompiledCodeObjectPrinter;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class TestSource extends TestSqueak {
    @Test
    public void testSource() {
        Object[] literals = new Object[]{14548994, image.nil, image.nil}; // header with numTemp=55
        // push true, push 1; storeIntoTemp i, quickReturnTop
        CompiledCodeObject code = makeMethod(
                        new byte[]{0x70, 0x68, 0x10, (byte) 0x8F, 0x10, 0x00, 0x02, 0x10, 0x7D, (byte) 0xC9, 0x7C},
                        literals);
        CharSequence source = CompiledCodeObjectPrinter.getString(code);
        //@formatter:off
		assertEquals(
            "1 <70> self\n" +
			"2 <68> popIntoTemp: 0\n" +
			"3 <10> pushTemp: 0\n" +
			"4 <8F 10 00 02> closureNumCopied: 1 numArgs: 0 bytes 7 to 9\n" +
			"5  <10> pushTemp: 0\n" +
			"6  <7D> blockReturn\n" +
			"7 <C9> send: value\n" +
			"8 <7C> returnTop", source);
		//@formatter:on
    }
}
