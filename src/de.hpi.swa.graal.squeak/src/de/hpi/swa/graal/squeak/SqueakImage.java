package de.hpi.swa.graal.squeak;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReader;
import de.hpi.swa.graal.squeak.interop.InteropArray;

@ExportLibrary(InteropLibrary.class)
public final class SqueakImage implements TruffleObject {
    private final SqueakImageContext image;

    public SqueakImage(final SqueakImageContext image) {
        this.image = image;
    }

    private static class SqueakImageNode extends RootNode {
        private final SqueakImage squeakImage;

        protected SqueakImageNode(final SqueakImage squeakImage) {
            super(squeakImage.image.getLanguage());
            this.squeakImage = squeakImage;
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            SqueakImageReader.load(squeakImage.image);
            return squeakImage;
        }
    }

    public RootCallTarget asCallTarget() {
        return Truffle.getRuntime().createCallTarget(new SqueakImageNode(this));
    }

    public String getName() {
        return image.getImagePath();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(final Object... arguments) {
        assert arguments.length == 0;
        image.interrupt.start();
        image.disableHeadless();
        return Truffle.getRuntime().createCallTarget(image.getActiveContextNode()).call();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true; // TODO: rework members interop integration and re-enable.
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isMemberReadable(@SuppressWarnings("unused") final String member) {
        return "Compiler".equals(member); // TODO: rework members interop integration and re-enable.
    }

    @ExportMessage
    public Object getMembers(final boolean includeInternal) {
        final Object[] members;
        members = new String[]{"Compiler"};
        return new InteropArray(members);
    }

    @ExportMessage
    public Object readMember(final String key) {
        if ("Compiler".equals(key)) {
            return image.getCompilerClass();
        } else {
            // TODO:
            return image.getGlobals();
        }
    }
}
