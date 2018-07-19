package de.hpi.swa.graal.squeak.nodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ParsingRequest;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageReaderNode;

public final class SqueakRootNode extends RootNode {
    private final SqueakLanguage language;

    @Child private SqueakImageReaderNode readerNode;

    public static SqueakRootNode create(final SqueakLanguage language, final ParsingRequest request) {
        return new SqueakRootNode(language, request);
    }

    private SqueakRootNode(final SqueakLanguage language, final ParsingRequest request) {
        super(language, new FrameDescriptor());
        this.language = language;
        try {
            final SqueakImageContext image = language.getContextReference().get();
            this.readerNode = new SqueakImageReaderNode(new FileInputStream(request.getSource().getPath()), image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        readerNode.executeRead(frame);
        return callImageContext();
    }

    @TruffleBoundary
    private Object callImageContext() {
        final SqueakImageContext image = language.getContextReference().get();
        image.interrupt.start();
        final DirectCallNode callNode;
        if (image.config.isCustomContext()) {
            callNode = DirectCallNode.create(image.getCustomContext());
        } else {
            callNode = DirectCallNode.create(image.getActiveContext());
        }
        return callNode.call(new Object[]{});
    }

}
