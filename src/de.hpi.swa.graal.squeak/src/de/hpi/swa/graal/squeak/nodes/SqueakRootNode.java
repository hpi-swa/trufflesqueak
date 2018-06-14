package de.hpi.swa.graal.squeak.nodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage.ParsingRequest;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageReader;

public final class SqueakRootNode extends RootNode {
    private final SqueakLanguage language;
    private final String imagePath;

    @Child SqueakImageReader readerNode;

    public static SqueakRootNode create(final SqueakLanguage language, final ParsingRequest request) {
        return new SqueakRootNode(language, request);
    }

    private SqueakRootNode(final SqueakLanguage language, final ParsingRequest request) {
        super(language, new FrameDescriptor());
        this.language = language;
        this.imagePath = request.getSource().getPath();
        try {
            final SqueakImageContext image = language.getContextReference().get();
            this.readerNode = new SqueakImageReader(new FileInputStream(imagePath), image);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            readerNode.executeRead(frame);
            return extracted();
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new SqueakException("SqueakRootNode should never return");
    }

    @TruffleBoundary
    private Object extracted() {
        System.exit(0);
        return null;
// final SqueakImageContext image = language.getContextReference().get();
// image.interrupt.start();
// final DirectCallNode callNode;
// if (image.config.isCustomContext()) {
// callNode = DirectCallNode.create(image.getCustomContext());
// } else {
// callNode = DirectCallNode.create(image.getActiveContext());
// }
// return callNode.call(new Object[]{});
    }

}
