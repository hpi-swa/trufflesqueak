/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interpreterproxy;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

import com.oracle.truffle.api.CompilerDirectives;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interpreterproxy.bindings.VirtualMachine;
import de.hpi.swa.trufflesqueak.io.SqueakDisplay;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectNewNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.util.LogUtils;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class InterpreterProxy implements AutoCloseable {
    private static final int BASE_HEADER_SIZE = 8;

    public static final InterpreterProxy SINGLETON = new InterpreterProxy();

    private SqueakImageContext context;
    private int numReceiverAndArguments;
    private Object[] receiverAndArguments;
    private int sp;
    private Arena arena;
    private final ArrayList<NativeObjectWrapper> postInvocationCleanups = new ArrayList<>();
    private final MemorySegment interpreterProxy = VirtualMachine.allocate(Arena.global());

    /* INTERPRETER VARIABLES */
    private final ArrayList<Object> objectRegistry = new ArrayList<>();
    private long primFailCode;

    /**
     * Initialization matches order in sqVirtualMachine.h.
     */
    private InterpreterProxy() {
        VirtualMachine.minorVersion(interpreterProxy, VirtualMachine.minorVersion.allocate(this::minorVersion, Arena.global()));
        VirtualMachine.majorVersion(interpreterProxy, VirtualMachine.majorVersion.allocate(this::majorVersion, Arena.global()));

        /* InterpreterProxy methodsFor: 'stack access' */

        VirtualMachine.pop(interpreterProxy, VirtualMachine.pop.allocate(this::pop, Arena.global()));
        VirtualMachine.popthenPush(interpreterProxy, VirtualMachine.popthenPush.allocate(this::popThenPush, Arena.global()));
        VirtualMachine.push(interpreterProxy, VirtualMachine.push.allocate(this::push, Arena.global()));
        VirtualMachine.pushBool(interpreterProxy, VirtualMachine.pushBool.allocate(this::pushBool, Arena.global()));
        VirtualMachine.pushInteger(interpreterProxy, VirtualMachine.pushInteger.allocate(this::pushInteger, Arena.global()));
        VirtualMachine.stackFloatValue(interpreterProxy, VirtualMachine.stackFloatValue.allocate(this::stackFloatValue, Arena.global()));
        VirtualMachine.stackIntegerValue(interpreterProxy, VirtualMachine.stackIntegerValue.allocate(this::stackIntegerValue, Arena.global()));
        VirtualMachine.stackObjectValue(interpreterProxy, VirtualMachine.stackObjectValue.allocate(this::stackObjectValue, Arena.global()));
        VirtualMachine.stackValue(interpreterProxy, VirtualMachine.stackValue.allocate(this::stackValue, Arena.global()));

        /* InterpreterProxy methodsFor: 'object access' */

        VirtualMachine.argumentCountOf(interpreterProxy, VirtualMachine.argumentCountOf.allocate(this::argumentCountOf, Arena.global()));
        VirtualMachine.arrayValueOf(interpreterProxy, VirtualMachine.arrayValueOf.allocate(this::arrayValueOf, Arena.global()));
        VirtualMachine.byteSizeOf(interpreterProxy, VirtualMachine.byteSizeOf.allocate(this::byteSizeOf, Arena.global()));
        VirtualMachine.fetchArrayofObject(interpreterProxy, VirtualMachine.fetchArrayofObject.allocate(this::fetchArrayofObject, Arena.global()));
        VirtualMachine.fetchClassOf(interpreterProxy, VirtualMachine.fetchClassOf.allocate(this::fetchClassOf, Arena.global()));
        VirtualMachine.fetchFloatofObject(interpreterProxy, VirtualMachine.fetchFloatofObject.allocate(this::fetchFloatofObject, Arena.global()));
        VirtualMachine.fetchIntegerofObject(interpreterProxy, VirtualMachine.fetchIntegerofObject.allocate(this::fetchIntegerOfObject, Arena.global()));
        VirtualMachine.fetchPointerofObject(interpreterProxy, VirtualMachine.fetchPointerofObject.allocate(this::fetchPointerOfObject, Arena.global()));
        VirtualMachine.firstFixedField(interpreterProxy, VirtualMachine.firstFixedField.allocate(this::firstFixedField, Arena.global()));
        VirtualMachine.firstIndexableField(interpreterProxy, VirtualMachine.firstIndexableField.allocate(this::firstIndexableField, Arena.global()));
        VirtualMachine.literalofMethod(interpreterProxy, VirtualMachine.literalofMethod.allocate(this::literalofMethod, Arena.global()));
        VirtualMachine.literalCountOf(interpreterProxy, VirtualMachine.literalCountOf.allocate(this::literalCountOf, Arena.global()));
        VirtualMachine.methodArgumentCount(interpreterProxy, VirtualMachine.methodArgumentCount.allocate(this::methodArgumentCount, Arena.global()));
        VirtualMachine.methodPrimitiveIndex(interpreterProxy, VirtualMachine.methodPrimitiveIndex.allocate(this::methodPrimitiveIndex, Arena.global()));
        VirtualMachine.primitiveIndexOf(interpreterProxy, VirtualMachine.primitiveIndexOf.allocate(this::primitiveIndexOf, Arena.global()));
        VirtualMachine.sizeOfSTArrayFromCPrimitive(interpreterProxy, VirtualMachine.sizeOfSTArrayFromCPrimitive.allocate(this::sizeOfSTArrayFromCPrimitive, Arena.global()));
        VirtualMachine.slotSizeOf(interpreterProxy, VirtualMachine.slotSizeOf.allocate(this::slotSizeOf, Arena.global()));
        VirtualMachine.stObjectat(interpreterProxy, VirtualMachine.stObjectat.allocate(this::stObjectat, Arena.global()));
        VirtualMachine.stObjectatput(interpreterProxy, VirtualMachine.stObjectatput.allocate(this::stObjectatput, Arena.global()));
        VirtualMachine.stSizeOf(interpreterProxy, VirtualMachine.stSizeOf.allocate(this::stSizeOf, Arena.global()));
        VirtualMachine.storeIntegerofObjectwithValue(interpreterProxy, VirtualMachine.storeIntegerofObjectwithValue.allocate(this::storeIntegerOfObjectWithValue, Arena.global()));
        VirtualMachine.storePointerofObjectwithValue(interpreterProxy, VirtualMachine.storePointerofObjectwithValue.allocate(this::storePointerofObjectwithValue, Arena.global()));

        /* InterpreterProxy methodsFor: 'testing' */

        VirtualMachine.isKindOf(interpreterProxy, VirtualMachine.isKindOf.allocate(this::isKindOf, Arena.global()));
        VirtualMachine.isMemberOf(interpreterProxy, VirtualMachine.isMemberOf.allocate(this::isMemberOf, Arena.global()));
        VirtualMachine.isBytes(interpreterProxy, VirtualMachine.isBytes.allocate(this::isBytes, Arena.global()));
        VirtualMachine.isFloatObject(interpreterProxy, VirtualMachine.isFloatObject.allocate(this::isFloatObject, Arena.global()));
        VirtualMachine.isIndexable(interpreterProxy, VirtualMachine.isIndexable.allocate(this::isIndexable, Arena.global()));
        VirtualMachine.isIntegerObject(interpreterProxy, VirtualMachine.isIntegerObject.allocate(this::isIntegerObject, Arena.global()));
        VirtualMachine.isIntegerValue(interpreterProxy, VirtualMachine.isIntegerValue.allocate(this::isIntegerValue, Arena.global()));
        VirtualMachine.isPointers(interpreterProxy, VirtualMachine.isPointers.allocate(this::isPointers, Arena.global()));
        VirtualMachine.isWeak(interpreterProxy, VirtualMachine.isWeak.allocate(this::isWeak, Arena.global()));
        VirtualMachine.isWords(interpreterProxy, VirtualMachine.isWords.allocate(this::isWords, Arena.global()));
        VirtualMachine.isWordsOrBytes(interpreterProxy, VirtualMachine.isWordsOrBytes.allocate(this::isWordsOrBytes, Arena.global()));

        /* InterpreterProxy methodsFor: 'converting' */

        VirtualMachine.booleanValueOf(interpreterProxy, VirtualMachine.booleanValueOf.allocate(this::booleanValueOf, Arena.global()));
        VirtualMachine.checkedIntegerValueOf(interpreterProxy, VirtualMachine.checkedIntegerValueOf.allocate(this::checkedIntegerValueOf, Arena.global()));
        VirtualMachine.floatObjectOf(interpreterProxy, VirtualMachine.floatObjectOf.allocate(this::floatObjectOf, Arena.global()));
        VirtualMachine.floatValueOf(interpreterProxy, VirtualMachine.floatValueOf.allocate(this::floatValueOf, Arena.global()));
        VirtualMachine.integerObjectOf(interpreterProxy, VirtualMachine.integerObjectOf.allocate(this::integerObjectOf, Arena.global()));
        VirtualMachine.integerValueOf(interpreterProxy, VirtualMachine.integerValueOf.allocate(this::integerValueOf, Arena.global()));
        VirtualMachine.positive32BitIntegerFor(interpreterProxy, VirtualMachine.positive32BitIntegerFor.allocate(this::positive32BitIntegerFor, Arena.global()));
        VirtualMachine.positive32BitValueOf(interpreterProxy, VirtualMachine.positive32BitValueOf.allocate(this::positive32BitValueOf, Arena.global()));

        /* InterpreterProxy methodsFor: 'special objects' */

        VirtualMachine.characterTable(interpreterProxy, VirtualMachine.characterTable.allocate(this::characterTable, Arena.global()));
        VirtualMachine.falseObject(interpreterProxy, VirtualMachine.falseObject.allocate(this::falseObject, Arena.global()));
        VirtualMachine.nilObject(interpreterProxy, VirtualMachine.nilObject.allocate(this::nilObject, Arena.global()));
        VirtualMachine.trueObject(interpreterProxy, VirtualMachine.trueObject.allocate(this::trueObject, Arena.global()));

        /* InterpreterProxy methodsFor: 'special classes' */

        VirtualMachine.classArray(interpreterProxy, VirtualMachine.classArray.allocate(this::classArray, Arena.global()));
        VirtualMachine.classBitmap(interpreterProxy, VirtualMachine.classBitmap.allocate(this::classBitmap, Arena.global()));
        VirtualMachine.classByteArray(interpreterProxy, VirtualMachine.classByteArray.allocate(this::classByteArray, Arena.global()));
        VirtualMachine.classCharacter(interpreterProxy, VirtualMachine.classCharacter.allocate(this::classCharacter, Arena.global()));
        VirtualMachine.classFloat(interpreterProxy, VirtualMachine.classFloat.allocate(this::classFloat, Arena.global()));
        VirtualMachine.classLargePositiveInteger(interpreterProxy, VirtualMachine.classLargePositiveInteger.allocate(this::classLargePositiveInteger, Arena.global()));
        VirtualMachine.classPoint(interpreterProxy, VirtualMachine.classPoint.allocate(this::classPoint, Arena.global()));
        VirtualMachine.classSemaphore(interpreterProxy, VirtualMachine.classSemaphore.allocate(this::classSemaphore, Arena.global()));
        VirtualMachine.classSmallInteger(interpreterProxy, VirtualMachine.classSmallInteger.allocate(this::classSmallInteger, Arena.global()));
        VirtualMachine.classString(interpreterProxy, VirtualMachine.classString.allocate(this::classString, Arena.global()));

        /* InterpreterProxy methodsFor: 'instance creation' */

        VirtualMachine.cloneObject(interpreterProxy, VirtualMachine.cloneObject.allocate(this::cloneObject, Arena.global()));
        VirtualMachine.instantiateClassindexableSize(interpreterProxy, VirtualMachine.instantiateClassindexableSize.allocate(this::instantiateClassIndexableSize, Arena.global()));
        VirtualMachine.makePointwithxValueyValue(interpreterProxy, VirtualMachine.makePointwithxValueyValue.allocate(this::makePointwithxValueyValue, Arena.global()));
        VirtualMachine.popRemappableOop(interpreterProxy, VirtualMachine.cloneObject.allocate(this::popRemappableOop, Arena.global()));
        VirtualMachine.pushRemappableOop(interpreterProxy, VirtualMachine.cloneObject.allocate(this::pushRemappableOop, Arena.global()));

        /* InterpreterProxy methodsFor: 'other' */

        VirtualMachine.becomewith(interpreterProxy, VirtualMachine.becomewith.allocate(this::becomewith, Arena.global()));
        VirtualMachine.byteSwapped(interpreterProxy, VirtualMachine.byteSwapped.allocate(this::byteSwapped, Arena.global()));
        VirtualMachine.failed(interpreterProxy, VirtualMachine.failed.allocate(this::failed, Arena.global()));
        VirtualMachine.fullDisplayUpdate(interpreterProxy, VirtualMachine.fullDisplayUpdate.allocate(this::fullDisplayUpdate, Arena.global()));
        VirtualMachine.fullGC(interpreterProxy, VirtualMachine.fullGC.allocate(this::fullGC, Arena.global()));
        VirtualMachine.incrementalGC(interpreterProxy, VirtualMachine.incrementalGC.allocate(this::incrementalGC, Arena.global()));
        VirtualMachine.primitiveFail(interpreterProxy, VirtualMachine.primitiveFail.allocate(this::primitiveFail, Arena.global()));
        VirtualMachine.showDisplayBitsLeftTopRightBottom(interpreterProxy, VirtualMachine.showDisplayBitsLeftTopRightBottom.allocate(this::showDisplayBitsLeftTopRightBottom, Arena.global()));
        VirtualMachine.signalSemaphoreWithIndex(interpreterProxy, VirtualMachine.signalSemaphoreWithIndex.allocate(this::signalSemaphoreWithIndex, Arena.global()));
        VirtualMachine.success(interpreterProxy, VirtualMachine.success.allocate(this::success, Arena.global()));
        VirtualMachine.superclassOf(interpreterProxy, VirtualMachine.superclassOf.allocate(this::superclassOf, Arena.global()));

        /* if VM_PROXY_MINOR > 13 */

        VirtualMachine.statNumGCs(interpreterProxy, VirtualMachine.statNumGCs.allocate(this::statNumGCs, Arena.global()));
        VirtualMachine.stringForCString(interpreterProxy, VirtualMachine.stringForCString.allocate(this::stringForCString, Arena.global()));

        /* InterpreterProxy methodsFor: 'BitBlt support' */

        /* if VM_PROXY_MINOR > 1 */

        VirtualMachine.loadBitBltFrom(interpreterProxy, VirtualMachine.loadBitBltFrom.allocate(this::loadBitBltFrom, Arena.global()));
        VirtualMachine.copyBits(interpreterProxy, VirtualMachine.copyBits.allocate(this::copyBits, Arena.global()));
        VirtualMachine.copyBitsFromtoat(interpreterProxy, VirtualMachine.copyBitsFromtoat.allocate(this::copyBitsFromtoat, Arena.global()));

        /* if VM_PROXY_MINOR > 2 */

        VirtualMachine.classLargeNegativeInteger(interpreterProxy, VirtualMachine.classLargeNegativeInteger.allocate(this::classLargeNegativeInteger, Arena.global()));
        VirtualMachine.signed32BitIntegerFor(interpreterProxy, VirtualMachine.signed32BitIntegerFor.allocate(this::signed32BitIntegerFor, Arena.global()));
        VirtualMachine.signed32BitValueOf(interpreterProxy, VirtualMachine.signed32BitValueOf.allocate(this::signed32BitValueOf, Arena.global()));
        VirtualMachine.includesBehaviorThatOf(interpreterProxy, VirtualMachine.includesBehaviorThatOf.allocate(this::includesBehaviorThatOf, Arena.global()));
        VirtualMachine.primitiveMethod(interpreterProxy, VirtualMachine.primitiveMethod.allocate(this::primitiveMethod, Arena.global()));

        /* InterpreterProxy methodsFor: 'FFI support' */

        VirtualMachine.classExternalAddress(interpreterProxy, VirtualMachine.classExternalAddress.allocate(this::classExternalAddress, Arena.global()));
        VirtualMachine.classExternalData(interpreterProxy, VirtualMachine.classExternalData.allocate(this::classExternalData, Arena.global()));
        VirtualMachine.classExternalFunction(interpreterProxy, VirtualMachine.classExternalFunction.allocate(this::classExternalFunction, Arena.global()));
        VirtualMachine.classExternalLibrary(interpreterProxy, VirtualMachine.classExternalLibrary.allocate(this::classExternalLibrary, Arena.global()));
        VirtualMachine.classExternalStructure(interpreterProxy, VirtualMachine.classExternalStructure.allocate(this::classExternalStructure, Arena.global()));
        VirtualMachine.ioLoadModuleOfLength(interpreterProxy, VirtualMachine.ioLoadModuleOfLength.allocate(this::ioLoadModuleOfLength, Arena.global()));
        VirtualMachine.ioLoadSymbolOfLengthFromModule(interpreterProxy, VirtualMachine.ioLoadSymbolOfLengthFromModule.allocate(this::ioLoadSymbolOfLengthFromModule, Arena.global()));
        VirtualMachine.isInMemory(interpreterProxy, VirtualMachine.isInMemory.allocate(this::isInMemory, Arena.global()));

        /* if VM_PROXY_MINOR > 3 */

        VirtualMachine.ioLoadFunctionFrom(interpreterProxy, VirtualMachine.ioLoadFunctionFrom.allocate(this::ioLoadFunctionFrom, Arena.global()));
        VirtualMachine.ioMicroMSecs(interpreterProxy, VirtualMachine.ioMicroMSecs.allocate(this::ioMicroMSecs, Arena.global()));

        /* if VM_PROXY_MINOR > 4 */

        VirtualMachine.positive64BitIntegerFor(interpreterProxy, VirtualMachine.positive64BitIntegerFor.allocate(this::positive64BitIntegerFor, Arena.global()));
        VirtualMachine.positive64BitValueOf(interpreterProxy, VirtualMachine.positive64BitValueOf.allocate(this::positive64BitValueOf, Arena.global()));
        VirtualMachine.signed64BitIntegerFor(interpreterProxy, VirtualMachine.signed64BitIntegerFor.allocate(this::signed64BitIntegerFor, Arena.global()));
        VirtualMachine.signed64BitValueOf(interpreterProxy, VirtualMachine.signed64BitValueOf.allocate(this::signed64BitValueOf, Arena.global()));

        /* if VM_PROXY_MINOR > 5 */

        VirtualMachine.isArray(interpreterProxy, VirtualMachine.isArray.allocate(this::isArray, Arena.global()));
        VirtualMachine.forceInterruptCheck(interpreterProxy, VirtualMachine.forceInterruptCheck.allocate(this::forceInterruptCheck, Arena.global()));

        /* if VM_PROXY_MINOR > 6 */

        VirtualMachine.fetchLong32ofObject(interpreterProxy, VirtualMachine.fetchLong32ofObject.allocate(this::fetchLong32OfObject, Arena.global()));
        VirtualMachine.getThisSessionID(interpreterProxy, VirtualMachine.getThisSessionID.allocate(this::getThisSessionID, Arena.global()));
        VirtualMachine.ioFilenamefromStringofLengthresolveAliases(interpreterProxy, VirtualMachine.ioFilenamefromStringofLengthresolveAliases.allocate(this::ioFilenamefromStringofLengthresolveAliases,
                        Arena.global()));
        VirtualMachine.vmEndianness(interpreterProxy, VirtualMachine.vmEndianness.allocate(this::vmEndianness, Arena.global()));

        /* if VM_PROXY_MINOR > 7 */

        VirtualMachine.callbackEnter(interpreterProxy, VirtualMachine.callbackEnter.allocate(this::callbackEnter, Arena.global()));

        /* if VM_PROXY_MINOR > 8 */

        VirtualMachine.primitiveFailFor(interpreterProxy, VirtualMachine.primitiveFailFor.allocate(this::primitiveFailFor, Arena.global()));
        VirtualMachine.setInterruptCheckChain(interpreterProxy, VirtualMachine.setInterruptCheckChain.allocate(this::setInterruptCheckChain, Arena.global()));
        VirtualMachine.classAlien(interpreterProxy, VirtualMachine.classAlien.allocate(this::classAlien, Arena.global()));
        VirtualMachine.classUnsafeAlien(interpreterProxy, VirtualMachine.classUnsafeAlien.allocate(this::classUnsafeAlien, Arena.global()));
        VirtualMachine.storeLong32ofObjectwithValue(interpreterProxy, VirtualMachine.storeLong32ofObjectwithValue.allocate(this::storeLong32OfObjectWithValue, Arena.global()));
        VirtualMachine.reestablishContextPriorToCallback(interpreterProxy, VirtualMachine.reestablishContextPriorToCallback.allocate(this::reestablishContextPriorToCallback, Arena.global()));
        VirtualMachine.getStackPointer(interpreterProxy, VirtualMachine.getStackPointer.allocate(this::getStackPointer, Arena.global()));
        VirtualMachine.isOopImmutable(interpreterProxy, VirtualMachine.isOopImmutable.allocate(this::isOopImmutable, Arena.global()));
        VirtualMachine.isOopMutable(interpreterProxy, VirtualMachine.isOopMutable.allocate(this::isOopMutable, Arena.global()));

        /* if VM_PROXY_MINOR > 13 */

        VirtualMachine.methodReturnBool(interpreterProxy, VirtualMachine.methodReturnBool.allocate(this::methodReturnBool, Arena.global()));
        VirtualMachine.methodReturnFloat(interpreterProxy, VirtualMachine.methodReturnFloat.allocate(this::methodReturnFloat, Arena.global()));
        VirtualMachine.methodReturnInteger(interpreterProxy, VirtualMachine.methodReturnInteger.allocate(this::methodReturnInteger, Arena.global()));
        VirtualMachine.methodReturnString(interpreterProxy, VirtualMachine.methodReturnString.allocate(this::methodReturnString, Arena.global()));
        VirtualMachine.methodReturnValue(interpreterProxy, VirtualMachine.methodReturnValue.allocate(this::methodReturnValue, Arena.global()));
        VirtualMachine.topRemappableOop(interpreterProxy, VirtualMachine.topRemappableOop.allocate(this::topRemappableOop, Arena.global()));

        /* if VM_PROXY_MINOR > 10 */

        VirtualMachine.disownVM(interpreterProxy, VirtualMachine.disownVM.allocate(this::disownVM, Arena.global()));
        VirtualMachine.ownVM(interpreterProxy, VirtualMachine.ownVM.allocate(this::ownVM, Arena.global()));
        VirtualMachine.addHighPriorityTickee(interpreterProxy, VirtualMachine.addHighPriorityTickee.allocate(this::addHighPriorityTickee, Arena.global()));
        VirtualMachine.addSynchronousTickee(interpreterProxy, VirtualMachine.addSynchronousTickee.allocate(this::addSynchronousTickee, Arena.global()));
        VirtualMachine.utcMicroseconds(interpreterProxy, VirtualMachine.utcMicroseconds.allocate(this::utcMicroseconds, Arena.global()));
        VirtualMachine.tenuringIncrementalGC(interpreterProxy, VirtualMachine.tenuringIncrementalGC.allocate(this::tenuringIncrementalGC, Arena.global()));
        VirtualMachine.isYoung(interpreterProxy, VirtualMachine.isYoung.allocate(this::isYoung, Arena.global()));
        VirtualMachine.isKindOfClass(interpreterProxy, VirtualMachine.isKindOfClass.allocate(this::isKindOfClass, Arena.global()));
        VirtualMachine.primitiveErrorTable(interpreterProxy, VirtualMachine.primitiveErrorTable.allocate(this::primitiveErrorTable, Arena.global()));
        VirtualMachine.primitiveFailureCode(interpreterProxy, VirtualMachine.primitiveFailureCode.allocate(this::primitiveFailureCode, Arena.global()));
        VirtualMachine.instanceSizeOf(interpreterProxy, VirtualMachine.instanceSizeOf.allocate(this::instanceSizeOf, Arena.global()));

        /* if VM_PROXY_MINOR > 11 */

        VirtualMachine.sendInvokeCallbackContext(interpreterProxy, VirtualMachine.sendInvokeCallbackContext.allocate(this::sendInvokeCallbackContext, Arena.global()));
        VirtualMachine.returnAsThroughCallbackContext(interpreterProxy, VirtualMachine.returnAsThroughCallbackContext.allocate(this::returnAsThroughCallbackContext, Arena.global()));
        VirtualMachine.signedMachineIntegerValueOf(interpreterProxy, VirtualMachine.signedMachineIntegerValueOf.allocate(this::signedMachineIntegerValueOf, Arena.global()));
        VirtualMachine.stackSignedMachineIntegerValue(interpreterProxy, VirtualMachine.stackSignedMachineIntegerValue.allocate(this::stackSignedMachineIntegerValue, Arena.global()));
        VirtualMachine.positiveMachineIntegerValueOf(interpreterProxy, VirtualMachine.positiveMachineIntegerValueOf.allocate(this::positiveMachineIntegerValueOf, Arena.global()));
        VirtualMachine.stackPositiveMachineIntegerValue(interpreterProxy, VirtualMachine.stackPositiveMachineIntegerValue.allocate(this::stackPositiveMachineIntegerValue, Arena.global()));
        VirtualMachine.getInterruptPending(interpreterProxy, VirtualMachine.getInterruptPending.allocate(this::getInterruptPending, Arena.global()));
        VirtualMachine.cStringOrNullFor(interpreterProxy, VirtualMachine.cStringOrNullFor.allocate(this::cStringOrNullFor, Arena.global()));
        VirtualMachine.startOfAlienData(interpreterProxy, VirtualMachine.startOfAlienData.allocate(this::startOfAlienData, Arena.global()));
        VirtualMachine.sizeOfAlienData(interpreterProxy, VirtualMachine.sizeOfAlienData.allocate(this::sizeOfAlienData, Arena.global()));
        VirtualMachine.signalNoResume(interpreterProxy, VirtualMachine.signalNoResume.allocate(this::signalNoResume, Arena.global()));

        /* if VM_PROXY_MINOR > 12 */

        VirtualMachine.isImmediate(interpreterProxy, VirtualMachine.isImmediate.allocate(this::isImmediate, Arena.global()));
        VirtualMachine.characterObjectOf(interpreterProxy, VirtualMachine.characterObjectOf.allocate(this::characterObjectOf, Arena.global()));
        VirtualMachine.characterValueOf(interpreterProxy, VirtualMachine.characterValueOf.allocate(this::characterValueOf, Arena.global()));
        VirtualMachine.isCharacterObject(interpreterProxy, VirtualMachine.isCharacterObject.allocate(this::isCharacterObject, Arena.global()));
        VirtualMachine.isCharacterValue(interpreterProxy, VirtualMachine.isCharacterValue.allocate(this::isCharacterValue, Arena.global()));
        VirtualMachine.isPinned(interpreterProxy, VirtualMachine.isPinned.allocate(this::isPinned, Arena.global()));
        VirtualMachine.pinObject(interpreterProxy, VirtualMachine.pinObject.allocate(this::pinObject, Arena.global()));
        VirtualMachine.unpinObject(interpreterProxy, VirtualMachine.unpinObject.allocate(this::unpinObject, Arena.global()));

        /* if VM_PROXY_MINOR > 13 */

        VirtualMachine.primitiveFailForOSError(interpreterProxy, VirtualMachine.primitiveFailForOSError.allocate(this::primitiveFailForOSError, Arena.global()));
        VirtualMachine.methodReturnReceiver(interpreterProxy, VirtualMachine.methodReturnReceiver.allocate(this::methodReturnReceiver, Arena.global()));
        VirtualMachine.primitiveFailForFFIExceptionat(interpreterProxy, VirtualMachine.primitiveFailForFFIExceptionat.allocate(this::primitiveFailForFFIExceptionat, Arena.global()));

        /* if VM_PROXY_MINOR > 14 */

        VirtualMachine.isBooleanObject(interpreterProxy, VirtualMachine.isBooleanObject.allocate(this::isBooleanObject, Arena.global()));
        VirtualMachine.isPositiveMachineIntegerObject(interpreterProxy, VirtualMachine.isPositiveMachineIntegerObject.allocate(this::isPositiveMachineIntegerObject, Arena.global()));

        /* if VM_PROXY_MINOR > 15 */

        VirtualMachine.classDoubleByteArray(interpreterProxy, VirtualMachine.classDoubleByteArray.allocate(this::classDoubleByteArray, Arena.global()));
        VirtualMachine.classWordArray(interpreterProxy, VirtualMachine.classWordArray.allocate(this::classWordArray, Arena.global()));
        VirtualMachine.classDoubleWordArray(interpreterProxy, VirtualMachine.classDoubleWordArray.allocate(this::classDoubleWordArray, Arena.global()));
        VirtualMachine.classFloat32Array(interpreterProxy, VirtualMachine.classFloat32Array.allocate(this::classFloat32Array, Arena.global()));
        VirtualMachine.classFloat64Array(interpreterProxy, VirtualMachine.classFloat64Array.allocate(this::classFloat64Array, Arena.global()));

        /* if VM_PROXY_MINOR > 16 */

        VirtualMachine.isShorts(interpreterProxy, VirtualMachine.isShorts.allocate(this::isShorts, Arena.global()));
        VirtualMachine.isLong64s(interpreterProxy, VirtualMachine.isLong64s.allocate(this::isLong64s, Arena.global()));
        VirtualMachine.identityHashOf(interpreterProxy, VirtualMachine.identityHashOf.allocate(this::identityHashOf, Arena.global()));
        VirtualMachine.isWordsOrShorts(interpreterProxy, VirtualMachine.isWordsOrShorts.allocate(this::isWordsOrShorts, Arena.global()));
        VirtualMachine.bytesPerElement(interpreterProxy, VirtualMachine.bytesPerElement.allocate(this::bytesPerElement, Arena.global()));
        VirtualMachine.fileTimesInUTC(interpreterProxy, VirtualMachine.fileTimesInUTC.allocate(this::fileTimesInUTC, Arena.global()));
        VirtualMachine.processOSErrInstVarOffset(interpreterProxy, VirtualMachine.processOSErrInstVarOffset.allocate(this::processOSErrInstVarOffset, Arena.global()));
        VirtualMachine.activeProcess(interpreterProxy, VirtualMachine.activeProcess.allocate(this::activeProcess, Arena.global()));
    }

    public InterpreterProxy newInvocation(final SqueakImageContext theContext, final Object[] theReceiverAndArguments) {
        context = theContext;
        numReceiverAndArguments = theReceiverAndArguments.length;
        receiverAndArguments = theReceiverAndArguments.clone();
        sp = numReceiverAndArguments;
        arena = Arena.ofConfined();
        return this;
    }

    @Override
    public void close() {
        postPrimitiveCleanups();
        context = null;
        numReceiverAndArguments = -1;
        receiverAndArguments = null;
        sp = -1;
        arena.close();
    }

    public Object getReturnValue() {
        return receiverAndArguments[0];
    }

    /* MISCELLANEOUS */

    public MemorySegment getPointer() {
        return interpreterProxy;
    }

    public void postPrimitiveCleanups() {
        postInvocationCleanups.forEach(NativeObjectWrapper::copyFromSegmentToStorage);
        postInvocationCleanups.clear();
    }

    private boolean hasSucceeded() {
        return failed() == 0;
    }

    /* OBJECT REGISTRY HELPERS */

    private Object objectRegistryGet(final long oop) {
        return objectRegistry.get((int) oop);
    }

    private int addObjectToRegistry(final Object object) {
        final int oop = objectRegistry.size();
        objectRegistry.add(object);
        return oop;
    }

    private int oopFor(final Object object) {
        for (int oop = 0; oop < objectRegistry.size(); oop++) {
            if (objectRegistry.get(oop) == object) {
                return oop;
            }
        }
        return addObjectToRegistry(object);
    }

    /* STACK HELPERS */

    private void pushObject(final Object object) {
        // push to the original stack pointer, as it always points to the slot where the next object
        // is pushed
        receiverAndArguments[sp++] = object;
    }

    private Object getObjectOnStack(final long reverseStackIndex) {
        if (reverseStackIndex < 0) {
            primitiveFail();
            return null;
        }
        // the stack pointer is the index of the object that is pushed onto the stack next,
        // so we subtract 1 to get the index of the object that was last pushed onto the stack
        final int stackIndex = sp - 1 - (int) reverseStackIndex;
        if (stackIndex < 0) {
            primitiveFail();
            return null;
        }
        final Object value = receiverAndArguments[stackIndex];
        assert value != null;
        return value;
    }

    private long methodReturnObject(final Object object) {
        assert hasSucceeded();
        sp = 0;
        receiverAndArguments[0] = object;
        return returnVoid();
    }

    private static long returnBoolean(final boolean bool) {
        return bool ? 1L : 0L;
    }

    private static long returnVoid() {
        return 0L;
    }

    /* CONVERSION HELPERS */

    private long objectToInteger(final Object object) {
        if (object instanceof final Long longObject) {
            return longObject;
        } else {
            LogUtils.INTERPRETER_PROXY.severe(() -> "Object to long called with non-Long: " + object);
            return primitiveFail();
        }
    }

    private double objectToFloat(final Object object) {
        if (object instanceof final FloatObject floatObject) {
            return floatObject.getValue();
        } else {
            LogUtils.INTERPRETER_PROXY.severe(() -> "Object to long called with non-FloatObject: " + object);
            return primitiveFail();
        }
    }

    private static Object integerToObject(final long integer) {
        return integer; // encoded as Long in TruffleSqueak
    }

    private static Object boolToObject(final boolean bool) {
        return BooleanObject.wrap(bool);
    }

    private static Object boolToObject(final long bool) {
        return boolToObject(bool != 0);
    }

    private static Object floatToObject(final double value) {
        return new FloatObject(value);
    }

    private NativeObject charPointerToByteString(final MemorySegment charPointer) {
        return context.asByteString(charPointerToBytes(charPointer));
    }

    private static byte[] charPointerToBytes(final MemorySegment charPointer) {
        return charPointer.getString(0).getBytes();
    }

    /* ACCESSING HELPERS */

    private static Object objectAt0(final Object object, final long index) {
        return SqueakObjectAt0Node.executeUncached(object, index);
    }

    /* TYPE CHECK HELPERS */

    private long instanceOfCheck(final long oop, final Class<?> klass) {
        final Object object = objectRegistryGet(oop);
        return returnBoolean(klass.isInstance(object));
    }

    private long nativeObjectCheck(final long oop, final Predicate<NativeObject> predicate) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof final NativeObject nativeObject) {
            return returnBoolean(predicate.test(nativeObject));
        }
        return returnVoid();
    }

    /* INTERPRETER PROXY METHODS */

    private long booleanValueOf(final long oop) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof final Boolean bool) {
            return returnBoolean(bool);
        } else {
            LogUtils.INTERPRETER_PROXY.severe(() -> "booleanValueOf called with non-Boolean object: " + object);
            return primitiveFail();
        }
    }

    private long byteSizeOf(final long oop) {
        if (oop < objectRegistry.size()) {
            if (objectRegistryGet(oop) instanceof final NativeObject nativeObject) {
                return nativeObject.byteSize();
            }
        } else {
            for (NativeObjectWrapper wrapper : postInvocationCleanups) {
                if (wrapper.segment.address() == oop + BASE_HEADER_SIZE) {
                    return wrapper.byteSizeOf();
                }
            }
        }
        // type is not supported (yet)
        return primitiveFail();
    }

    private long classAlien() {
        return oopFor(context.getAlienClass());
    }

    private long classArray() {
        return oopFor(context.arrayClass);
    }

    private long classBitmap() {
        return oopFor(context.bitmapClass);
    }

    private long classByteArray() {
        return oopFor(context.byteArrayClass);
    }

    private long classCharacter() {
        return oopFor(context.characterClass);
    }

    private long classDoubleByteArray() {
        return oopFor(context.getDoubleByteArrayClass());
    }

    private long classDoubleWordArray() {
        return oopFor(context.getDoubleWordArrayClass());
    }

    private long classExternalAddress() {
        return oopFor(context.getExternalAddressClass());
    }

    private long classExternalData() {
        return oopFor(context.getExternalDataClass());
    }

    private long classExternalFunction() {
        return oopFor(context.getExternalFunctionClass());
    }

    private long classExternalLibrary() {
        return oopFor(context.getExternalLibraryClass());
    }

    private long classExternalStructure() {
        return oopFor(context.getExternalStructureClass());
    }

    private long classFloat() {
        return oopFor(context.floatClass);
    }

    private long classFloat32Array() {
        return oopFor(context.lookup("FloatArray"));
    }

    private long classFloat64Array() {
        return oopFor(context.lookup("Float64Array"));
    }

    private long classLargeNegativeInteger() {
        return oopFor(context.largeNegativeIntegerClass);
    }

    private long classLargePositiveInteger() {
        return oopFor(context.largePositiveIntegerClass);
    }

    private long classPoint() {
        return oopFor(context.pointClass);
    }

    private long classSemaphore() {
        return oopFor(context.semaphoreClass);
    }

    private long classSmallInteger() {
        return oopFor(context.smallIntegerClass);
    }

    private long classString() {
        return oopFor(context.byteStringClass);
    }

    private long classUnsafeAlien() {
        return oopFor(context.getUnsafeAlienClass());
    }

    private long classWordArray() {
        return oopFor(context.getWordArrayClass());
    }

    public long failed() {
        return primFailCode;
    }

    private long falseObject() {
        return oopFor(BooleanObject.FALSE);
    }

    private long fetchIntegerOfObject(final long fieldIndex, final long objectPointer) {
        return objectToInteger(objectAt0(objectRegistryGet(objectPointer), fieldIndex));
    }

    private long fetchLong32OfObject(final long fieldIndex, final long oop) {
        return (int) fetchIntegerOfObject(fieldIndex, oop);
    }

    private long fetchPointerOfObject(final long index, final long oop) {
        return oopFor(objectAt0(objectRegistryGet(oop), index));
    }

    private MemorySegment firstIndexableField(final long oop) {
        if (objectRegistryGet(oop) instanceof final NativeObject nativeObject) {
            final NativeObjectWrapper wrapper = NativeObjectWrapper.from(nativeObject, arena);
            if (wrapper == null) {
                return MemorySegment.NULL;
            }
            postInvocationCleanups.add(wrapper);
            return wrapper.segment;
        } else {
            assert false;
            return MemorySegment.NULL;
        }
    }

    private long floatObjectOf(final double value) {
        return oopFor(floatToObject(value));
    }

    private double floatValueOf(final long oop) {
        return objectToFloat(objectRegistryGet(oop));
    }

    private long instanceSizeOf(final long classPointer) {
        final Object object = objectRegistryGet(classPointer);
        if (object instanceof final ClassObject classObject) {
            return classObject.getBasicInstanceSize();
        } else {
            LogUtils.INTERPRETER_PROXY.severe(() -> "instanceSizeOf called with non-ClassObject: " + object);
            return primitiveFail();
        }
    }

    private long instantiateClassIndexableSize(final long classPointer, final long size) {
        final Object object = objectRegistryGet(classPointer);
        if (object instanceof final ClassObject classObject) {
            final AbstractSqueakObject newObject = SqueakObjectNewNode.executeUncached(classObject, MiscUtils.toIntExact(size));
            return oopFor(newObject);
        } else {
            LogUtils.INTERPRETER_PROXY.severe(() -> "instantiateClassIndexableSize called with non-ClassObject: " + object);
            return primitiveFail();
        }
    }

    private long integerObjectOf(final long value) {
        return oopFor(integerToObject(value));
    }

    private long integerValueOf(final long oop) {
        return objectToInteger(objectRegistryGet(oop));
    }

    private MemorySegment ioLoadFunctionFrom(final MemorySegment functionName, final MemorySegment moduleName) {
        /* TODO */
        LogUtils.INTERPRETER_PROXY.severe(() -> "Missing implementation for ioLoadFunctionFrom: %s>>%s".formatted(charPointerToByteString(functionName), charPointerToByteString(moduleName)));
        return MemorySegment.NULL;
    }

    private long isArray(final long oop) {
        return instanceOfCheck(oop, ArrayObject.class);
    }

    private long isBooleanObject(final long oop) {
        return returnBoolean(objectRegistryGet(oop) instanceof Boolean);
    }

    private long isBytes(final long oop) {
        return nativeObjectCheck(oop, NativeObject::isByteType);
    }

    private long isIntegerObject(final long oop) {
        return returnBoolean(objectRegistryGet(oop) instanceof Long);
    }

    private long isKindOf(final long oop, final MemorySegment classNamePointer) {
        final byte[] className = charPointerToBytes(classNamePointer);
        final Object value = objectRegistryGet(oop);
        ClassObject currentClass = SqueakObjectClassNode.executeUncached(value);
        while (currentClass != null) {
            if (classNameOfIs(currentClass, className)) {
                return returnBoolean(true);
            }
            currentClass = currentClass.getResolvedSuperclass();
        }
        return returnBoolean(false);
    }

    private static boolean classNameOfIs(final ClassObject classObject, final byte[] className) {
        if (classObject.size() >= CLASS.NAME && classObject.getOtherPointers()[CLASS.NAME] instanceof final NativeObject nativeObject && nativeObject.isByteType()) {
            return Arrays.equals(className, nativeObject.getByteStorage());
        } else {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    private long isPinned(@SuppressWarnings("unused") final long oop) {
        return returnBoolean(false); // always false, pinning not yet supported
    }

    private long isPointers(final long oop) {
        return instanceOfCheck(oop, AbstractPointersObject.class);
    }

    private long isPositiveMachineIntegerObject(final long oop) {
        final Object object = objectRegistryGet(oop);
        if (object instanceof final Long integer) {
            return returnBoolean(integer >= 0L);
        }
        if (object instanceof final NativeObject largeInteger) {
            return returnBoolean(LargeIntegers.isZeroOrPositive(SqueakImageContext.getSlow(), largeInteger) && LargeIntegers.fitsIntoLong(largeInteger));
        }
        return returnBoolean(false);
    }

    private long isWords(final long oop) {
        return nativeObjectCheck(oop, NativeObject::isLongType);
    }

    private long isWordsOrBytes(final long oop) {
        return nativeObjectCheck(oop, no -> no.isIntType() || no.isByteType());
    }

    private long majorVersion() {
        return 1L;
    }

    private long methodArgumentCount() {
        return numReceiverAndArguments - 1;
    }

    private long methodReturnBool(final long bool) {
        return methodReturnObject(boolToObject(bool));
    }

    private long methodReturnFloat(final double value) {
        return methodReturnObject(floatToObject(value));
    }

    private long methodReturnInteger(final long integer) {
        return methodReturnObject(integerToObject(integer));
    }

    private long methodReturnReceiver() {
        assert hasSucceeded();
        pop(numReceiverAndArguments - 1); // leave the receiver on the stack
        return returnVoid();
    }

    private long methodReturnString(final MemorySegment pointer) {
        return methodReturnObject(charPointerToByteString(pointer));
    }

    private long methodReturnValue(final long oop) {
        return methodReturnObject(objectRegistryGet(oop));
    }

    private long minorVersion() {
        return 17L;
    }

    private long nilObject() {
        return oopFor(NilObject.SINGLETON);
    }

    private long pop(final long nItems) {
        if (sp < (int) nItems) {
            return primitiveFail();
        }
        sp -= (int) nItems;
        return returnVoid();
    }

    private long popThenPush(final long nItems, final long oop) {
        pop(nItems);
        push(oop);
        return returnVoid();
    }

    private long positive32BitIntegerFor(final long integerValue) {
        return integerObjectOf(integerValue & Integer.MAX_VALUE);
    }

    private long positive32BitValueOf(final long oop) {
        return integerValueOf(oop) & Integer.MAX_VALUE;
    }

    private long positive64BitIntegerFor(final long integerValue) {
        return integerObjectOf(Math.abs(integerValue));
    }

    private long positive64BitValueOf(final long oop) {
        return Math.abs(integerValueOf(oop));
    }

    private long primitiveFail() {
        if (primFailCode == 0) {
            primitiveFailFor(1L);
        }
        return returnVoid();
    }

    private long primitiveFailFor(final long reasonCode) {
        LogUtils.INTERPRETER_PROXY.fine(() -> "Primitive failed with code: " + reasonCode);
        return primFailCode = reasonCode;
    }

    private long push(final long oop) {
        pushObject(objectRegistryGet(oop));
        return returnVoid();
    }

    private long pushBool(final long integer) {
        pushObject(boolToObject(integer));
        return returnVoid();
    }

    private long pushInteger(final long integer) {
        pushObject(integerToObject(integer));
        return returnVoid();
    }

    private long showDisplayBitsLeftTopRightBottom(final long aFormOop, final long l, final long t, final long r, final long b) {
        final Object aFormObject = objectRegistryGet(aFormOop);
        if (aFormObject instanceof final PointersObject aForm) {
            final SqueakDisplay display = context.getDisplay();
            if (display != null) {
                display.showDisplayBits(aForm, (int) l, (int) t, (int) r, (int) b);
            }
        }
        return returnVoid();
    }

    private long signed32BitIntegerFor(final long integerValue) {
        return integerObjectOf((int) integerValue);
    }

    private int signed32BitValueOf(final long oop) {
        return (int) integerValueOf(oop);
    }

    private long slotSizeOf(final long oop) {
        final Object value = objectRegistryGet(oop);
        if (value instanceof final NativeObject nativeObject) {
            return NativeObjectSizeNode.executeUncached(nativeObject);
        } else {
            LogUtils.INTERPRETER_PROXY.warning(() -> "slotSizeOf called with non-NativeObject: " + value);
            return returnVoid();
        }
    }

    private double stackFloatValue(final long offset) {
        final Object value = getObjectOnStack(offset);
        if (value instanceof final Double doubleObject) {
            return doubleObject;
        } else {
            return primitiveFail();
        }
    }

    private long stackIntegerValue(final long offset) {
        final Object value = getObjectOnStack(offset);
        if (value instanceof final Long longObject) {
            return longObject;
        } else {
            return primitiveFail();
        }
    }

    private long stackObjectValue(final long offset) {
        final Object value = getObjectOnStack(offset);
        if (value instanceof Long) {
            return primitiveFail();
        } else {
            return oopFor(value);
        }
    }

    private long stackValue(final long offset) {
        return oopFor(getObjectOnStack(offset));
    }

    private long statNumGCs() {
        return MiscUtils.getCollectionCount();
    }

    private long stringForCString(final MemorySegment object) {
        return oopFor(charPointerToByteString(object));
    }

    private long stSizeOf(final long oop) {
        return SqueakObjectSizeNode.executeUncached(objectRegistryGet(oop));
    }

    private long success(final long successBoolean) {
        if (successBoolean == 0) {
            primitiveFail();
        }
        return returnVoid();
    }

    private long trueObject() {
        return oopFor(BooleanObject.TRUE);
    }

    /* NOT YET IMPLEMENTED  */

    @SuppressWarnings("unused")
    private long argumentCountOf(final long methodPointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment arrayValueOf(final long oop) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long fetchClassOf(final long oop) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment fetchArrayofObject(final long fieldIndex, final long objectPointer) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private double fetchFloatofObject(final long fieldIndex, final long objectPointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment firstFixedField(final long oop) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long literalofMethod(final long offset, final long methodPointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long literalCountOf(final long methodPointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long methodPrimitiveIndex() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long primitiveIndexOf(final long methodPointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isMemberOf(final long oop, final MemorySegment aString) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long sizeOfSTArrayFromCPrimitive(final MemorySegment cPtr) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long stObjectat(final long array, final long fieldIndex) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long stObjectatput(final long array, final long fieldIndex, final long value) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long storePointerofObjectwithValue(final long fieldIndex, final long oop, final long valuePointer) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isIndexable(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isIntegerValue(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isWeak(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long checkedIntegerValueOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long characterTable() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long cloneObject(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long makePointwithxValueyValue(final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long popRemappableOop(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long pushRemappableOop(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long becomewith(final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long byteSwapped(final long l) {
        assert false; // TODO
        return -1;
    }

    private long fullDisplayUpdate() {
        assert false; // TODO
        return -1;
    }

    private void fullGC() {
        assert false; // TODO
    }

    private void incrementalGC() {
        assert false; // TODO
    }

    @SuppressWarnings("unused")
    private long signalSemaphoreWithIndex(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long superclassOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long loadBitBltFrom(final long l) {
        assert false; // TODO
        return -1;
    }

    private long copyBits() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long copyBitsFromtoat(final long l, final long l1, final long l2) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long includesBehaviorThatOf(final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    private long primitiveMethod() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment ioLoadModuleOfLength(final long l, final long l1) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private MemorySegment ioLoadSymbolOfLengthFromModule(final long l, final long l1, final MemorySegment segment) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long isInMemory(final long l) {
        assert false; // TODO
        return -1;
    }

    private int ioMicroMSecs() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long signed64BitIntegerFor(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long signed64BitValueOf(final long l) {
        assert false; // TODO
        return -1;
    }

    private void forceInterruptCheck() {
        assert false; // TODO
    }

    private long getThisSessionID() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long ioFilenamefromStringofLengthresolveAliases(final MemorySegment segment, final MemorySegment segment1, final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    private long vmEndianness() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private void storeIntegerOfObjectWithValue(final long index, final long oop, final long integer) {
        assert false; // TODO
        LogUtils.INTERPRETER_PROXY.warning(() -> "Missing implementation for storeIntegerOfObjectWithValue: %s, %s, %s".formatted(index, oop, integer));
    }

    @SuppressWarnings("unused")
    private long storeLong32OfObjectWithValue(final long fieldIndex, final long oop, final long anInteger) {
        assert false; // TODO
        LogUtils.INTERPRETER_PROXY.warning(() -> "Missing implementation for storeLong32OfObjectWithValue: %s, %s, %s".formatted(fieldIndex, oop, anInteger));
        return returnVoid();
    }

    @SuppressWarnings("unused")
    private long callbackEnter(final MemorySegment segment) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment setInterruptCheckChain(final MemorySegment segment) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long reestablishContextPriorToCallback(final long l) {
        assert false; // TODO
        return -1;
    }

    private MemorySegment getStackPointer() {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long isOopImmutable(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isOopMutable(final long l) {
        assert false; // TODO
        return -1;
    }

    private long topRemappableOop() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long disownVM(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long ownVM(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private void addHighPriorityTickee(final MemorySegment segment, final int i) {
        assert false; // TODO
    }

    @SuppressWarnings("unused")
    private void addSynchronousTickee(final MemorySegment segment, final int i, final int i1) {
        assert false; // TODO
    }

    private long utcMicroseconds() {
        assert false; // TODO
        return -1;
    }

    private void tenuringIncrementalGC() {
        assert false; // TODO
    }

    @SuppressWarnings("unused")
    private long isYoung(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isKindOfClass(final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    private long primitiveErrorTable() {
        assert false; // TODO
        return -1;
    }

    private long primitiveFailureCode() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long sendInvokeCallbackContext(final MemorySegment segment) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long returnAsThroughCallbackContext(final long l, final MemorySegment segment, final long l1) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long signedMachineIntegerValueOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long stackSignedMachineIntegerValue(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long positiveMachineIntegerValueOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long stackPositiveMachineIntegerValue(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long getInterruptPending() {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private MemorySegment cStringOrNullFor(final long l) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private MemorySegment startOfAlienData(final long l) {
        assert false; // TODO
        return MemorySegment.NULL;
    }

    @SuppressWarnings("unused")
    private long sizeOfAlienData(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long signalNoResume(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isImmediate(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long characterObjectOf(final int i) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isFloatObject(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long characterValueOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isCharacterObject(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isCharacterValue(final int i) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long pinObject(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long unpinObject(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long primitiveFailForOSError(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long primitiveFailForFFIExceptionat(final long l, final long l1) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isShorts(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isLong64s(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long identityHashOf(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long isWordsOrShorts(final long l) {
        assert false; // TODO
        return -1;
    }

    @SuppressWarnings("unused")
    private long bytesPerElement(final long l) {
        assert false; // TODO
        return -1;
    }

    private long fileTimesInUTC() {
        assert false; // TODO
        return -1;
    }

    private long processOSErrInstVarOffset() {
        assert false; // TODO
        return -1;
    }

    private long activeProcess() {
        assert false; // TODO
        return -1;
    }
}
