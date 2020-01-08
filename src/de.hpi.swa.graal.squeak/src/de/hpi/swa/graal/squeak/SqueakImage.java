/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
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
import de.hpi.swa.graal.squeak.model.NilObject;

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
        image.attachDisplayIfNecessary();
        return Truffle.getRuntime().createCallTarget(image.getActiveContextNode()).call();
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isMemberReadable(@SuppressWarnings("unused") final String member) {
        return image.lookup(member) != NilObject.SINGLETON;
    }

    @ExportMessage
    public Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return image.evaluate("Smalltalk globals keys collect: [:ea | ea asString]");
    }

    @ExportMessage
    public Object readMember(final String member) {
        return image.lookup(member);
    }
}
