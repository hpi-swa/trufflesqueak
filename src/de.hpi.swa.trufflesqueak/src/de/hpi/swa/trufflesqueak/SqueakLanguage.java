/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak;

import org.graalvm.options.OptionDescriptors;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.SqueakFileDetector;
import de.hpi.swa.trufflesqueak.interop.SqueakLanguageView;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

@TruffleLanguage.Registration(//
                characterMimeTypes = {SqueakLanguageConfig.MIME_TYPE, SqueakLanguageConfig.ST_MIME_TYPE}, //
                defaultMimeType = SqueakLanguageConfig.ST_MIME_TYPE, //
                dependentLanguages = {"nfi"}, //
                fileTypeDetectors = SqueakFileDetector.class, //
                id = SqueakLanguageConfig.ID, //
                implementationName = SqueakLanguageConfig.IMPLEMENTATION_NAME, //
                interactive = true, //
                internal = false, //
                name = SqueakLanguageConfig.NAME, //
                version = SqueakLanguageConfig.VERSION, //
                website = SqueakLanguageConfig.WEBSITE)
@ProvidedTags({StandardTags.StatementTag.class, StandardTags.CallTag.class, StandardTags.RootTag.class, DebuggerTags.AlwaysHalt.class})
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {
    private static final LanguageReference<SqueakLanguage> REFERENCE = LanguageReference.create(SqueakLanguage.class);

    public static SqueakLanguage get(final AbstractNode node) {
        return REFERENCE.get(node);
    }

    @Override
    protected SqueakImageContext createContext(final Env env) {
        return new SqueakImageContext(this, env);
    }

    @Override
    protected void finalizeContext(final SqueakImageContext context) {
        context.finalizeContext();
    }

    @Override
    protected CallTarget parse(final ParsingRequest request) {
        final SqueakImageContext image = SqueakImageContext.getSlow();
        final Source source = request.getSource();
        final String imageOrSourcePath = source.getCharacters().toString();
        if (SqueakLanguageConfig.IMAGE_SOURCE_NAME.equals(source.getName())) {
            image.setImagePath(imageOrSourcePath);
            return image.getSqueakImage().asCallTarget();
        } else {
            image.ensureLoaded();
            return image.getDoItContextNode(request).getCallTarget();
        }
    }

    @Override
    protected boolean isThreadAccessAllowed(final Thread thread, final boolean singleThreaded) {
        return true; // TODO: Experimental, make TruffleSqueak work in multiple threads.
    }

    @Override
    protected Object getScope(final SqueakImageContext context) {
        return context.getScope();
    }

    public String getTruffleLanguageHome() {
        return getLanguageHome();
    }

    @Override
    protected OptionDescriptors getOptionDescriptors() {
        return SqueakOptions.createDescriptors();
    }

    @Override
    protected boolean patchContext(final SqueakImageContext context, final Env newEnv) {
        return context.patch(newEnv);
    }

    @Override
    protected Object getLanguageView(final SqueakImageContext context, final Object value) {
        return SqueakLanguageView.create(value);
    }
}
