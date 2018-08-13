package de.hpi.swa.graal.squeak.nodes.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.ForeignAccess;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.MESSAGE;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class PolyglotPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public boolean isEnabled(final SqueakImageContext image) {
        return image.supportsTruffleObject();
    }

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

        @TruffleBoundary
        @Specialization(guards = {"receiver.isByteType()", "languageNameObj.isByteType()"})
        protected final Object doParseAndCall(final NativeObject receiver, final NativeObject languageNameObj) {
            final String languageName = languageNameObj.asString();
            if (!code.image.env.getLanguages().containsKey(languageName)) {
                throw new PrimitiveFailed();
            }
            final String foreignCode = receiver.asString();
            try {
                final Source source = Source.newBuilder(foreignCode).language(languageName).name("eval").build();
                final CallTarget foreignCallTarget = code.image.env.parse(source);
                try {
                    return code.image.wrap(foreignCallTarget.call());
                } finally {
                    PrimLastErrorNode.lastErrorMessage = null;
                }
            } catch (Throwable e) {
                CompilerDirectives.transferToInterpreter();
                PrimLastErrorNode.lastErrorMessage = e.toString();
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primEvalC")
    protected abstract static class PrimEvalCNode extends AbstractPrimitiveNode {
        private static final String C_FILENAME = "temp.c";
        private static final String LLVM_FILENAME = "temp.bc";
        @Child private Node readNode = Message.READ.createNode();
        @Child private Node executeNode = Message.createExecute(0).createNode();

        protected PrimEvalCNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"receiver.isByteType()", "memberToCall.isByteType()"})
        @TruffleBoundary
        protected final Object doEvaluate(final NativeObject receiver, final NativeObject memberToCall) {
            final String foreignCode = receiver.asString();
            final String cFile = code.image.imageRelativeFilePathFor(C_FILENAME);
            final String llvmFile = code.image.imageRelativeFilePathFor(LLVM_FILENAME);
            try {
                Files.write(Paths.get(cFile), foreignCode.getBytes());
                final Process p = Runtime.getRuntime().exec("clang -O1 -c -emit-llvm -o " + llvmFile + " " + cFile);
                p.waitFor();
                final Source source = Source.newBuilder(new File(llvmFile)).language("llvm").name("eval").build();
                final CallTarget foreignCallTarget = code.image.env.parse(source);
                final TruffleObject library = (TruffleObject) foreignCallTarget.call();
                final TruffleObject cFunction;
                try {
                    cFunction = (TruffleObject) ForeignAccess.sendRead(readNode, library, memberToCall.asString());
                } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                    PrimLastErrorNode.lastErrorMessage = e.toString();
                    throw new PrimitiveFailed();
                }
                final Object result;
                try {
                    result = ForeignAccess.sendExecute(executeNode, cFunction);
                } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                    PrimLastErrorNode.lastErrorMessage = e.toString();
                    throw new PrimitiveFailed();
                }
                return code.image.wrap(result);
            } catch (IOException | RuntimeException | InterruptedException e) {
                PrimLastErrorNode.lastErrorMessage = e.toString();
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primGetKeysMessage")
    protected abstract static class PrimGetKeysNode extends AbstractPrimitiveNode {
        @Child private Node keysNode = Message.KEYS.createNode();
        @Child private Node getSizeNode = Message.GET_SIZE.createNode();
        @Child private Node readNode = Message.READ.createNode();

        protected PrimGetKeysNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "!isAbstractSqueakObject(receiver)")
        protected final Object doKeys(final TruffleObject receiver) {
            try {
                final TruffleObject sendKeysResult = ForeignAccess.sendKeys(keysNode, receiver);
                final int size = (int) ForeignAccess.sendGetSize(getSizeNode, sendKeysResult);
                final Object[] keys = new Object[size];
                for (int index = 0; index < size; index++) {
                    keys[index] = ForeignAccess.sendRead(readNode, receiver, index);
                }
                return code.image.wrap(keys);
            } catch (UnsupportedMessageException | UnknownIdentifierException e) {
                code.image.printToStdErr(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primGetSizeMessage")
    protected abstract static class PrimGetSizeNode extends AbstractPrimitiveNode {
        @Child private Node getSizeNode = Message.GET_SIZE.createNode();

        protected PrimGetSizeNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "!isAbstractSqueakObject(receiver)")
        protected final Object doSize(final TruffleObject receiver) {
            try {
                final Object sendGetSize = ForeignAccess.sendGetSize(getSizeNode, receiver);
                return code.image.wrap(sendGetSize);
            } catch (UnsupportedMessageException e) {
                code.image.printToStdErr(e);
                return 0L;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primLastError")
    protected abstract static class PrimLastErrorNode extends AbstractPrimitiveNode {
        protected static String lastErrorMessage = null;

        protected PrimLastErrorNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "lastErrorMessage != null")
        protected final Object doError(@SuppressWarnings("unused") final Object receiver) {
            try {
                return code.image.wrap(lastErrorMessage);
            } catch (NullPointerException e) {
                return code.image.nil;
            }
        }

        @Specialization(guards = "lastErrorMessage == null")
        protected final Object doNil(@SuppressWarnings("unused") final Object receiver) {
            return code.image.nil;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primEnvGetLanguages")
    protected abstract static class PrimListLanguagesNode extends AbstractPrimitiveNode {
        protected PrimListLanguagesNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doList(@SuppressWarnings("unused") final ClassObject receiver) {
            final Object[] languageStrings = code.image.env.getLanguages().keySet().toArray();
            return code.image.wrap(languageStrings);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primReadMessage")
    protected abstract static class PrimReadMessageNode extends AbstractPrimitiveNode {
        @Child private Node readNode = Message.READ.createNode();

        protected PrimReadMessageNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "selector.isByteType()"})
        protected final Object doRead(final TruffleObject receiver, final NativeObject selector) {
            try {
                return code.image.wrap(ForeignAccess.sendRead(readNode, receiver, selector.asString()));
            } catch (UnknownIdentifierException | UnsupportedMessageException e) {
                code.image.printToStdErr(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primSendMessage")
    protected abstract static class PrimSendMessageNode extends AbstractPrimitiveNode {

        protected PrimSendMessageNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "message.getSqClass() == code.image.messageClass"})
        protected final Object doSend(final TruffleObject receiver, final PointersObject message) {
            final String selectorString = ((NativeObject) message.at0(MESSAGE.SELECTOR)).asString();
            final String identifier;
            final int endIndex = selectorString.indexOf(':');
            if (endIndex >= 0) {
                identifier = selectorString.substring(0, endIndex);
            } else {
                identifier = selectorString;
            }
            final Object[] arguments = ((PointersObject) message.at0(MESSAGE.ARGUMENTS)).getPointers();
            final Node invokeNode = Message.createInvoke(arguments.length).createNode();
            try {
                return code.image.wrap(ForeignAccess.sendInvoke(invokeNode, receiver, identifier, arguments));
            } catch (UnsupportedTypeException | ArityException | UnknownIdentifierException | UnsupportedMessageException e) {
                code.image.printToStdErr(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primStringRepresentation")
    protected abstract static class PrimStringRepresentationNode extends AbstractPrimitiveNode {
        @Child private Node readNode = Message.READ.createNode();

        protected PrimStringRepresentationNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        @TruffleBoundary
        protected final Object doRead(final TruffleObject receiver) {
            return code.image.wrap(receiver.toString());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primUnboxMessage")
    protected abstract static class PrimUnboxMessageNode extends AbstractPrimitiveNode {
        @Child private Node unboxNode = Message.UNBOX.createNode();

        protected PrimUnboxMessageNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)"})
        protected final Object doRead(final TruffleObject receiver) {
            try {
                return code.image.wrap(ForeignAccess.sendUnbox(unboxNode, receiver));
            } catch (UnsupportedMessageException e) {
                code.image.printToStdErr(e);
                throw new PrimitiveFailed();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primWriteMessage")
    protected abstract static class PrimWriteMessageNode extends AbstractPrimitiveNode {
        @Child private Node writeNode = Message.WRITE.createNode();

        protected PrimWriteMessageNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"!isAbstractSqueakObject(receiver)", "selector.isByteType()"})
        protected final Object doWrite(final TruffleObject receiver, final NativeObject selector, final Object value) {
            try {
                return code.image.wrap(ForeignAccess.sendWrite(writeNode, receiver, selector.asString(), value));
            } catch (UnknownIdentifierException | UnsupportedMessageException | UnsupportedTypeException e) {
                code.image.printToStdErr(e);
                throw new PrimitiveFailed();
            }
        }
    }
}
