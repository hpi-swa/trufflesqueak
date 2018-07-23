package de.hpi.swa.graal.squeak.nodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.oracle.truffle.api.TruffleLanguage;
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

    @Child private RootNode executeNode;
    @Child private IndirectCallNode indirectCallNode = IndirectCallNode.create();

    public static SqueakRootNode create(final SqueakLanguage language, final ParsingRequest request) {
        return new SqueakRootNode(language, request);
    }

    private SqueakRootNode(final SqueakLanguage language, final ParsingRequest request) {
        super(language, new FrameDescriptor());
        image = language.getContextReference().get();
        executeNode = new SqueakLoadImageNode(language, image, request.getSource().getPath());
    }

    public static final class SqueakLoadImageNode extends RootNode {
        private final SqueakImageContext image;
        @Child private SqueakImageReaderNode readerNode;

        public SqueakLoadImageNode(final TruffleLanguage<?> language, final SqueakImageContext image, final String imagePath) {
            super(language);
            this.image = image;
            try {
                this.readerNode = new SqueakImageReaderNode(new FileInputStream(imagePath), image);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            readerNode.executeRead(frame);
            image.interrupt.start();
            return null;
        }

    }

    @Override
    public Object execute(final VirtualFrame frame) {
        executeNode.execute(frame);
        final ExecuteTopLevelContextNode node = image.config.isCustomContext() ? image.getCustomContext() : image.getActiveContext();
        return executeNode.replace(node).execute(frame);
    }
}
