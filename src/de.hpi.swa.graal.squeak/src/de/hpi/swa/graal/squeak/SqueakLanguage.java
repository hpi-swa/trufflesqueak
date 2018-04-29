package de.hpi.swa.graal.squeak;

import java.io.FileInputStream;
import java.io.PrintWriter;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.dsl.UnsupportedSpecializationException;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;

@TruffleLanguage.Registration(name = SqueakLanguage.NAME, version = "0.1", mimeType = SqueakLanguage.MIME_TYPE)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.RootTag.class, StandardTags.StatementTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {
    public static final String MIME_TYPE = "application/x-squeak-smalltalk";
    public static final String NAME = "Squeak/Smalltalk";

    @Override
    protected SqueakImageContext createContext(final Env env) {
        final PrintWriter out = new PrintWriter(env.out(), true);
        final PrintWriter err = new PrintWriter(env.err(), true);
        return new SqueakImageContext(this, env, out, err);
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        final SqueakImageContext image = this.getContextReference().get();
        image.fillInFrom(new FileInputStream(request.getSource().getPath()));
        if (image.config.isCustomContext()) {
            return image.getCustomContext();
        } else {
            return image.getActiveContext();
        }
    }

    @Override
    protected boolean isObjectOfLanguage(final Object object) {
        return object instanceof BaseSqueakObject;
    }

    @Override
    protected Object findMetaObject(final SqueakImageContext image, final Object value) {
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

    public static SqueakImageContext getContext() {
        return null; // FIXME
    }
}
