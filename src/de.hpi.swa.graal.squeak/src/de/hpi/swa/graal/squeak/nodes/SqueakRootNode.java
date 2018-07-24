package de.hpi.swa.graal.squeak.nodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage.ParsingRequest;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageReaderNode;

public final class SqueakRootNode extends RootNode {
    private final SqueakImageContext image;

    @Child private SqueakImageReaderNode readerNode;
    @Child private IndirectCallNode indirectCallNode = IndirectCallNode.create();

    public static SqueakRootNode create(final SqueakLanguage language, final ParsingRequest request) {
        return new SqueakRootNode(language, request);
    }

    private SqueakRootNode(final SqueakLanguage language, final ParsingRequest request) {
        super(language, new FrameDescriptor());
        image = language.getContextReference().get();
        try {
            this.readerNode = new SqueakImageReaderNode(new FileInputStream(request.getSource().getPath()), image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        readerNode.executeRead(frame);
        image.interrupt.start();
        final CallTarget callTarget = image.config.isCustomContext() ? image.getCustomContext() : image.getActiveContext();
        return indirectCallNode.call(callTarget, new Object[]{});
    }
}
