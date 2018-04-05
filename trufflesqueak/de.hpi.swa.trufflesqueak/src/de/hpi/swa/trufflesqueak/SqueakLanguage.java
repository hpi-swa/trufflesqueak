package de.hpi.swa.trufflesqueak;

import java.io.FileInputStream;
import java.io.PrintWriter;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

@TruffleLanguage.Registration(name = SqueakLanguage.NAME, version = "0.1", mimeType = SqueakLanguage.MIME_TYPE)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.RootTag.class, StandardTags.StatementTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {
    public static final String MIME_TYPE = "application/x-squeak-smalltalk";
    public static final String NAME = "Squeak/Smalltalk";

    @Override
    protected SqueakImageContext createContext(Env env) {
        PrintWriter out = new PrintWriter(env.out(), true);
        PrintWriter err = new PrintWriter(env.err(), true);
        return new SqueakImageContext(this, env, out, err);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        SqueakImageContext image = this.getContextReference().get();
        image.fillInFrom(new FileInputStream(request.getSource().getPath()));
        if (image.config.isCustomContext()) {
            return image.getCustomContext();
        } else {
            return image.getActiveContext();
        }
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof BaseSqueakObject;
    }

    @Override
    protected Object findMetaObject(SqueakImageContext image, Object value) {
        // TODO: return ContextObject instead?
        if (value instanceof FrameMarker) {
            return image.nilClass;
        }
        try {
            return SqueakLookupClassNode.create(image).executeLookup(value);
        } catch (UnsupportedSpecializationException e) {
            return null;
        }
    }

    public static final SqueakImageContext getContext() {
        return null; // FIXME
    }
}
