package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.interop.ValueToSqueakObjectNode;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return PolyglotPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primParseAndCall")
    protected abstract static class PrimEvaluateAsNode extends AbstractPrimitiveNode {

        protected PrimEvaluateAsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doParseAndCall(final NativeObject receiver, final NativeObject languageNameObj) {
            final String languageName = languageNameObj.toString();
            if (!code.image.env.getLanguages().containsKey(languageName)) {
                throw new PrimitiveFailed();
            }
            final String foreignCode = receiver.toString();
            try {
                final Source source = Source.newBuilder(foreignCode).language(languageName).name("eval").build();
                final CallTarget foreignCallTarget = code.image.env.parse(source);
                return foreignCallTarget.call();
            } catch (RuntimeException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primEvalC")
    protected abstract static class PrimEvalCNode extends AbstractPrimitiveNode {
        @CompilationFinal private static final String C_FILENAME = "temp.c";
        @CompilationFinal private static final String LLVM_FILENAME = "temp.bc";
        @Child private ValueToSqueakObjectNode toSqueakNode;

        protected PrimEvalCNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
            toSqueakNode = ValueToSqueakObjectNode.create(code);
        }

        @Specialization
        protected final Object doEvaluate(final NativeObject receiver, final NativeObject memberToCall) {
            final String foreignCode = receiver.toString();
            final String cFile = code.image.imageRelativeFilePathFor(C_FILENAME);
            final String llvmFile = code.image.imageRelativeFilePathFor(LLVM_FILENAME);
            try {
                Files.write(Paths.get(cFile), foreignCode.getBytes());
                final Process p = Runtime.getRuntime().exec("clang -O1 -c -emit-llvm -o " + llvmFile + " " + cFile);
                p.waitFor();
                final Context polyglot = Context.newBuilder().allowAllAccess(true).build();
                final org.graalvm.polyglot.Source source = org.graalvm.polyglot.Source.newBuilder("llvm", new File(llvmFile)).build();
                final Value result = polyglot.eval(source).getMember(memberToCall.toString()).execute();
                return toSqueakNode.executeValue(result);
            } catch (IOException | RuntimeException | InterruptedException e) {
                e.printStackTrace();
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primEnvGetLanguages")
    protected abstract static class PrimListLanguagesNode extends AbstractPrimitiveNode {
        protected PrimListLanguagesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final Object doList(@SuppressWarnings("unused") final ClassObject receiver) {
            final Object[] languageStrings = code.image.env.getLanguages().keySet().toArray();
            return code.image.wrap(languageStrings);
        }
    }
}
