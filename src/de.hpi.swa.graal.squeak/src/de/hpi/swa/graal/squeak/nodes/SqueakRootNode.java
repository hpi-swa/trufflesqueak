package de.hpi.swa.graal.squeak.nodes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
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
    private final String imagePath;

    @Child private IndirectCallNode indirectCallNode = IndirectCallNode.create();

    public static SqueakRootNode create(final SqueakLanguage language, final ParsingRequest request) {
        return new SqueakRootNode(language, request);
    }

    private SqueakRootNode(final SqueakLanguage language, final ParsingRequest request) {
        super(language, new FrameDescriptor());
        image = language.getContextReference().get();
        imagePath = request.getSource().getPath();
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        CompilerDirectives.transferToInterpreter();
        try {
            indirectCallNode.call(Truffle.getRuntime().createCallTarget(new SqueakImageReaderNode(new FileInputStream(imagePath), image)), new Object[0]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        image.interrupt.start();
        final ExecuteTopLevelContextNode executeNode = image.config.isCustomContext() ? image.getCustomContext() : image.getActiveContext();
        return indirectCallNode.call(Truffle.getRuntime().createCallTarget(executeNode), new Object[0]);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
