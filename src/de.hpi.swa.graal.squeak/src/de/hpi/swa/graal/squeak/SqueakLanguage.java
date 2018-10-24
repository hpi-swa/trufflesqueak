package de.hpi.swa.graal.squeak;

import java.util.ArrayList;
import java.util.Arrays;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageReaderNode;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.nodes.context.LookupClassNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@TruffleLanguage.Registration(id = SqueakLanguageConfig.ID, name = SqueakLanguageConfig.NAME, version = SqueakLanguageConfig.VERSION, mimeType = SqueakLanguageConfig.MIME_TYPE, interactive = true, internal = false)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.RootTag.class, StandardTags.StatementTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {

    @Override
    protected SqueakImageContext createContext(final Env env) {
        final SqueakImageContext context = new SqueakImageContext(this, env);
        context.getOutput().println("== Running " + SqueakLanguageConfig.NAME + " on " + getRuntimeName() + " ==");
        return context;
    }

    @Override
    protected void initializeContext(final SqueakImageContext context) {
        Truffle.getRuntime().createCallTarget(new SqueakImageReaderNode(context)).call();
    }

    public static String getRuntimeName() {
        return Truffle.getRuntime().getName() + " (Java " + System.getProperty("java.version") + ")";
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        final SqueakImageContext image = getContext();
        if (request.getSource().isInteractive()) {
            image.interrupt.start();
            return Truffle.getRuntime().createCallTarget(image.getActiveContext());
        } else {
            return Truffle.getRuntime().createCallTarget(image.getCustomContext(request.getSource().getCharacters()));
        }
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
        return LookupClassNode.create(image).executeLookup(value);
    }

    @Override
    protected Iterable<Scope> findTopScopes(final SqueakImageContext context) {
        return Arrays.asList(Scope.newBuilder("Smalltalk", context.getSmalltalkDictionary()).build());
    }

    @Override
    protected Iterable<Scope> findLocalScopes(final SqueakImageContext context, final Node node, final Frame frame) {
        // TODO Implement for LSP
        final ArrayList<Scope> scopes = new ArrayList<>();
        for (Scope s : super.findLocalScopes(context, node, frame)) {
            scopes.add(s);
        }
        if (frame != null) {
            // do stuff with frame (read stack slots)
        }
        return scopes;
    }

    @Override
    protected SourceSection findSourceLocation(final SqueakImageContext context, final Object value) {
        // TODO Implement for LSP -> "go-to-definition within same workspace window"
        return super.findSourceLocation(context, value);
    }

    public static SqueakImageContext getContext() {
        return getCurrentContext(SqueakLanguage.class);
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return SqueakOptions.createDescriptors();
    }

    @Override
    protected boolean patchContext(final SqueakImageContext context, final Env newEnv) {
        return context.patch(newEnv);
    }
}
