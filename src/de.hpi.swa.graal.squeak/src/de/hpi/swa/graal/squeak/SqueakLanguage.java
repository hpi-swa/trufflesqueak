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
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageReaderNode;
import de.hpi.swa.graal.squeak.interop.SqueakFileDetector;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;
import de.hpi.swa.graal.squeak.nodes.SqueakGuards;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.MiscUtils;

@TruffleLanguage.Registration(//
                byteMimeTypes = SqueakLanguageConfig.MIME_TYPE, //
                characterMimeTypes = SqueakLanguageConfig.ST_MIME_TYPE, //
                defaultMimeType = SqueakLanguageConfig.ST_MIME_TYPE, //
                fileTypeDetectors = SqueakFileDetector.class, //
                id = SqueakLanguageConfig.ID, //
                implementationName = SqueakLanguageConfig.IMPLEMENTATION_NAME, //
                interactive = true, //
                internal = false, //
                name = SqueakLanguageConfig.NAME, //
                version = SqueakLanguageConfig.VERSION)
@ProvidedTags({StandardTags.CallTag.class, StandardTags.RootTag.class, StandardTags.StatementTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {

    @Override
    protected SqueakImageContext createContext(final Env env) {
        return new SqueakImageContext(this, env);
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) throws Exception {
        final SqueakImageContext image = getContext();
        final Source source = request.getSource();
        if (source.hasBytes()) {
            image.setImagePath(source.getPath());
            return Truffle.getRuntime().createCallTarget(new SqueakImageReaderNode(image));
        } else {
            image.ensureLoaded();
            if (source.isInternal()) {
                image.printToStdOut(MiscUtils.format("Evaluating '%s'...", source.getCharacters().toString()));
            }
            return Truffle.getRuntime().createCallTarget(image.getDoItContextNode(source));
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
        return LookupClassNode.getUncached().executeLookup(WrapToSqueakNode.getUncached().executeWrap(value));
    }

    @Override
    protected Iterable<Scope> findTopScopes(final SqueakImageContext context) {
        context.ensureLoaded();
        return Arrays.asList(Scope.newBuilder("Smalltalk", context.getGlobals()).build());
    }

    @Override
    protected Iterable<Scope> findLocalScopes(final SqueakImageContext context, final Node node, final Frame frame) {
        // TODO Implement for LSP
        final ArrayList<Scope> scopes = new ArrayList<>();
        for (final Scope s : super.findLocalScopes(context, node, frame)) {
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
