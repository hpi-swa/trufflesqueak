package de.hpi.swa.graal.squeak;

import java.io.PrintWriter;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;

import de.hpi.swa.graal.squeak.config.SqueakConfig;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.SqueakRootNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;

@TruffleLanguage.Registration(id = SqueakConfig.ID, name = SqueakConfig.NAME, version = SqueakConfig.VERSION, mimeType = SqueakConfig.MIME_TYPE, interactive = true, internal = false)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.RootTag.class, StandardTags.StatementTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {

    @Override
    protected SqueakImageContext createContext(final Env env) {
        final PrintWriter out = new PrintWriter(env.out(), true);
        final PrintWriter err = new PrintWriter(env.err(), true);
        out.println("== Running " + SqueakConfig.NAME + " on " + getRuntimeName() + " ==");
        return new SqueakImageContext(this, env, out, err);
    }

    public static String getRuntimeName() {
        return Truffle.getRuntime().getName() + " (Java " + System.getProperty("java.version") + ")";
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        return Truffle.getRuntime().createCallTarget(SqueakRootNode.create(this, request));
    }

    @Override
    protected boolean isObjectOfLanguage(final Object object) {
        return SqueakGuards.isAbstractSqueakObject(object);
    }

    @Override
    protected Object findMetaObject(final SqueakImageContext image, final Object value) {
        // TODO: return ContextObject instead?
        if (value instanceof FrameMarker) {
            return image.nilClass;
        }
        return SqueakLookupClassNode.create(image).executeLookup(value);
    }

    public static SqueakImageContext getContext() {
        return getCurrentContext(SqueakLanguage.class);
    }
}
