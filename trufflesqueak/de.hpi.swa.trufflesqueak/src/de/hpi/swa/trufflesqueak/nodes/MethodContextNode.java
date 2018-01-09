package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.ConditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.JumpBytecodes.UnconditionalJumpNode;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;
import de.hpi.swa.trufflesqueak.util.FrameMarker;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class MethodContextNode extends RootNode {
    @CompilationFinal private final FrameMarker frameMarker = new FrameMarker();
    @CompilationFinal private MethodContextObject cachedContext;
    @CompilationFinal private final CompiledCodeObject code;
    @CompilationFinal private BlockClosureObject closure;
    @Children private final AbstractBytecodeNode[] bytecodeNodes;
    @Child private PushStackNode pushNode;
    @Child private SendSelectorNode aboutToReturnNode;
    @Child private FrameArgumentNode argumentNode = new FrameArgumentNode(1);

    public MethodContextNode(SqueakLanguage language, MethodContextObject context, CompiledCodeObject code) {
        this(language, context, code, null);
    }

    public MethodContextNode(SqueakLanguage language, CompiledCodeObject code) {
        this(language, null, code, null);
    }

    public MethodContextNode(SqueakLanguage language, CompiledBlockObject code, BlockClosureObject closure) {
        this(language, null, code, closure);
    }

    public MethodContextNode(SqueakLanguage language, MethodContextObject context, CompiledCodeObject code, BlockClosureObject closure) {
        super(language, code.getFrameDescriptor());
        this.cachedContext = context;
        this.code = code;
        this.closure = closure;
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
        pushNode = new PushStackNode(code);
        BaseSqueakObject aboutToReturnSelector = (BaseSqueakObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorAboutToReturn);
        aboutToReturnNode = new SendSelectorNode(code, -1, -1, aboutToReturnSelector, 2);
    }

    private void enterFrame(VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setObject(code.markerSlot, frameMarker);
        frame.setObject(code.methodSlot, code);
        frame.setObject(code.thisContextSlot, cachedContext);
        // sp points to the last temp slot
        int sp = initialSP();
        assert sp >= -1;
        frame.setInt(code.stackPointerSlot, sp);
        frame.setObject(code.closureSlot, closure != null ? closure : code.image.nil);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        enterFrame(frame);
        MethodContextObject context;
        try {
            executeBytecode(frame);
            CompilerDirectives.transferToInterpreter();
            throw new RuntimeException("Method did not return");
        } catch (LocalReturn lr) {
            context = getContextObject();
            if (context.isDirty()) {
                MethodContextObject sender = context.getSender();
                context.unwind();
                throw new NonVirtualReturn(lr.getReturnValue(), sender, sender);
            } else {
                context.unwind();
                return lr.getReturnValue();
            }
        } catch (NonLocalReturn nlr) {
            context = getContextObject();
            if (context.isUnwindMarked()) { // handle ensure: or ifCurtailed:
                pushNode.executeWrite(frame, nlr.getTargetContext());
                pushNode.executeWrite(frame, nlr.getReturnValue());
                pushNode.executeWrite(frame, context);
                context.atput0(CONTEXT.TEMP_FRAME_START, argumentNode.executeGeneric(frame)); // store ensure BlockClosure in context
                aboutToReturnNode.executeSend(frame);
            }
            if (context.isDirty()) {
                MethodContextObject sender = context.getSender();
                context.unwind();
                throw new NonVirtualReturn(nlr.getReturnValue(), nlr.getTargetContext(), sender);
            } else {
                context.unwind();
                if (nlr.getTargetContext().hasSameMethodObject(context)) {
                    return nlr.getReturnValue();
                } else {
                    throw nlr;
                }
            }
        }
    }

    /*
     * Inspired by Sulong's LLVMDispatchBasicBlockNode (https://goo.gl/4LMzfX).
     */
    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    protected void executeBytecode(VirtualFrame frame) {
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        int pc = initialPC();
        int backJumpCounter = 0;
        try {
            while (pc >= 0) {
                CompilerAsserts.partialEvaluationConstant(pc);
                AbstractBytecodeNode node = bytecodeNodes[pc];
                if (node instanceof ConditionalJumpNode) {
                    ConditionalJumpNode jumpNode = (ConditionalJumpNode) node;
                    boolean condition = jumpNode.executeCondition(frame);
                    if (CompilerDirectives.injectBranchProbability(jumpNode.getBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR), condition)) {
                        int successor = jumpNode.getJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.TRUE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                                code.image.interrupt.checkForInterrupts(frame);
                            }
                        }
                        pc = successor;
                        continue;
                    } else {
                        int successor = jumpNode.getNoJumpSuccessor();
                        if (CompilerDirectives.inInterpreter()) {
                            jumpNode.increaseBranchProbability(ConditionalJumpNode.FALSE_SUCCESSOR);
                            if (successor <= pc) {
                                backJumpCounter++;
                                code.image.interrupt.checkForInterrupts(frame);
                            }
                        }
                        pc = successor;
                        continue;
                    }
                } else if (node instanceof UnconditionalJumpNode) {
                    int successor = ((UnconditionalJumpNode) node).getJumpSuccessor();
                    if (CompilerDirectives.inInterpreter()) {
                        if (successor <= pc) {
                            backJumpCounter++;
                            code.image.interrupt.checkForInterrupts(frame);
                        }
                    }
                    pc = successor;
                    continue;
                } else {
                    pc = node.executeInt(frame);
                    continue;
                }
            }
        } finally {
            assert backJumpCounter >= 0;
            LoopNode.reportLoopCount(this, backJumpCounter);
        }
    }

    private int initialPC() {
        if (cachedContext == null) {
            return 0;
        }
        CompiledCodeObject codeObject = (CompiledCodeObject) cachedContext.at0(CONTEXT.METHOD);
        int rawPC = (int) cachedContext.at0(CONTEXT.INSTRUCTION_POINTER);
        if (rawPC < 0) {
            return 0;
        }
        return rawPC - codeObject.getBytecodeOffset() - 1;
    }

    private int initialSP() {
        // no need to read context.at0(CONTEXT.STACKPOINTER)
        return code.getNumTemps() - 1;
    }

    @Override
    public String toString() {
        return code.toString();
    }

    @Override
    public String getName() {
        return code.toString();
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return tag == StandardTags.RootTag.class;
    }

    public MethodContextObject getContextObject() {
        if (cachedContext == null) {
            cachedContext = createContextObject();
        }
        return cachedContext;
    }

    public MethodContextObject createContextObject() {
        if (cachedContext == null) {
            Frame frame = Truffle.getRuntime().getCurrentFrame().getFrame(FrameInstance.FrameAccess.MATERIALIZE);
            MethodContextObject activeContext = MethodContextObject.createReadOnlyContextObject(code.image, frame);
            cachedContext = MethodContextObject.createWriteableContextObject(code.image, code.frameSize());
            cachedContext.atput0(CONTEXT.METHOD, code);
            cachedContext.atput0(CONTEXT.SENDER, activeContext, false);
            cachedContext.atput0(CONTEXT.INSTRUCTION_POINTER, -1); // FIXME: pc? cachedContext.getCodeObject().getBytecodeOffset() + 1
            cachedContext.atput0(CONTEXT.RECEIVER, frame.getArguments()[0]);
            cachedContext.atput0(CONTEXT.CLOSURE_OR_NIL, code.image.nil);
        }
        return cachedContext;
    }
}
