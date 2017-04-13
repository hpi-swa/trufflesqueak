package de.hpi.swa.trufflesqueak;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

@TruffleLanguage.Registration(name = "Squeak", version = "0.1", mimeType = SqueakLanguage.MIME_TYPE)
public final class SqueakLanguage extends TruffleLanguage<SqueakImageContext> {
    public static final String MIME_TYPE = "application/x-squeak";

    @Override
    protected SqueakImageContext createContext(Env env) {
        BufferedReader in = new BufferedReader(new InputStreamReader(env.in()));
        PrintWriter out = new PrintWriter(env.out(), true);
        return new SqueakImageContext(env, in, out);
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        Reader reader = request.getSource().getReader();
        SqueakImageContext image = this.getContextReference().get();
        image.fillInFrom(reader);
        return Truffle.getRuntime().createCallTarget(image.getActiveContext(this));
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return object instanceof BaseSqueakObject;
    }

    @Override
    protected Object findExportedSymbol(SqueakImageContext context, String globalName, boolean onlyExplicit) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Object getLanguageGlobal(SqueakImageContext context) {
        return context.smalltalk;
    }
}
