/*
 * Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
#include <stdlib.h>

#define VM_PROXY_MAJOR 1
#define VM_PROXY_MINOR 17
#include "sqPlatformSpecific.h"
#include "sqVirtualMachine.h"

EXPORT(VirtualMachine*) createInterpreterProxy(
    // sorted alphabetically, identical to getExecutables in
    // src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/nodes/plugins/ffi/InterpreterProxy.java
	sqInt (*booleanValueOf)(sqInt oop),
	sqInt (*byteSizeOf)(sqInt oop),
	sqInt (*classAlien)(void),
	sqInt (*classArray)(void),
	sqInt (*classBitmap)(void),
	sqInt (*classByteArray)(void),
	sqInt (*classCharacter)(void),
	sqInt (*classDoubleByteArray)(void),
	sqInt (*classDoubleWordArray)(void),
	sqInt (*classExternalAddress)(void),
	sqInt (*classExternalData)(void),
	sqInt (*classExternalFunction)(void),
	sqInt (*classExternalLibrary)(void),
	sqInt (*classExternalStructure)(void),
	sqInt (*classFloat)(void),
	sqInt (*classFloat32Array)(void),
	sqInt (*classFloat64Array)(void),
	sqInt (*classLargeNegativeInteger)(void),
	sqInt (*classLargePositiveInteger)(void),
	sqInt (*classPoint)(void),
	sqInt (*classSemaphore)(void),
	sqInt (*classSmallInteger)(void),
	sqInt (*classString)(void),
	sqInt (*classUnsafeAlien)(void),
	sqInt (*classWordArray)(void),
	sqInt (*failed)(void),
	sqInt (*falseObject)(void),
	sqInt (*fetchIntegerofObject)(sqInt fieldIndex, sqInt objectPointer),
	sqInt (*fetchLong32ofObject)(sqInt fieldIndex, sqInt oop),
	sqInt (*fetchPointerofObject)(sqInt index, sqInt oop),
	void *(*firstIndexableField)(sqInt oop),
	double (*floatValueOf)(sqInt oop),
	sqInt (*instanceSizeOf)(sqInt aClass),
	sqInt (*instantiateClassindexableSize)(sqInt classPointer, sqInt size),
	sqInt (*integerObjectOf)(sqInt value),
	sqInt (*integerValueOf)(sqInt oop),
	void *(*ioLoadFunctionFrom)(char *functionName, char *moduleName),
	sqInt (*isArray)(sqInt oop),
	sqInt (*isBooleanObject)(sqInt oop),
	sqInt (*isBytes)(sqInt oop),
	sqInt (*isIntegerObject)(sqInt oop),
	sqInt (*isKindOf)(sqInt oop, char *aString),
	sqInt (*isPinned)(sqInt oop),
	sqInt (*isPointers)(sqInt oop),
	sqInt (*isPositiveMachineIntegerObject)(sqInt oop),
	sqInt (*isWords)(sqInt oop),
	sqInt (*isWordsOrBytes)(sqInt oop),
	sqInt (*majorVersion)(void),
	sqInt (*methodArgumentCount)(void),
	sqInt (*methodReturnBool)(sqInt value),
	sqInt (*methodReturnFloat)(double value),
	sqInt (*methodReturnInteger)(sqInt value),
	sqInt (*methodReturnReceiver)(void),
	sqInt (*methodReturnString)(char *value),
	sqInt (*methodReturnValue)(sqInt oop),
	sqInt (*minorVersion)(void),
	sqInt (*nilObject)(void),
	sqInt (*pop)(sqInt nItems),
	sqInt (*popthenPush)(sqInt nItems, sqInt oop),
	sqInt (*positive32BitIntegerFor)(unsigned int integerValue),
	usqInt (*positive32BitValueOf)(sqInt oop),
	sqInt (*positive64BitIntegerFor)(usqLong integerValue),
	usqLong (*positive64BitValueOf)(sqInt oop),
	sqInt (*primitiveFail)(void),
	sqInt (*primitiveFailFor)(sqInt reasonCode),
	sqInt (*push)(sqInt object),
	sqInt (*pushInteger)(sqInt integerValue),
	sqInt (*showDisplayBitsLeftTopRightBottom)(sqInt aForm, sqInt l, sqInt t, sqInt r, sqInt b),
	sqInt (*signed32BitIntegerFor)(sqInt integerValue),
	int (*signed32BitValueOf)(sqInt oop),
	sqInt (*slotSizeOf)(sqInt oop),
	sqInt (*stackIntegerValue)(sqInt offset),
	sqInt (*stackObjectValue)(sqInt offset),
	sqInt (*stackValue)(sqInt offset),
	sqInt (*statNumGCs)(void),
	sqInt (*storeIntegerofObjectwithValue)(sqInt index, sqInt oop, sqInt integer),
	usqInt (*storeLong32ofObjectwithValue)(sqInt fieldIndex, sqInt oop, usqInt anInteger),
	sqInt (*stringForCString)(char* nullTerminatedCString),
	sqInt (*stSizeOf)(sqInt oop),
	sqInt (*success)(sqInt aBoolean),
	sqInt (*trueObject)(void)
) {
	VirtualMachine* interpreterProxy = (VirtualMachine*)calloc(1, sizeof(VirtualMachine));

	interpreterProxy->booleanValueOf = booleanValueOf;
	interpreterProxy->byteSizeOf = byteSizeOf;
	interpreterProxy->classAlien = classAlien;
	interpreterProxy->classArray = classArray;
	interpreterProxy->classBitmap = classBitmap;
	interpreterProxy->classByteArray = classByteArray;
	interpreterProxy->classCharacter = classCharacter;
	interpreterProxy->classDoubleByteArray = classDoubleByteArray;
	interpreterProxy->classDoubleWordArray = classDoubleWordArray;
	interpreterProxy->classExternalAddress = classExternalAddress;
	interpreterProxy->classExternalData = classExternalData;
	interpreterProxy->classExternalFunction = classExternalFunction;
	interpreterProxy->classExternalLibrary = classExternalLibrary;
	interpreterProxy->classExternalStructure = classExternalStructure;
	interpreterProxy->classFloat = classFloat;
	interpreterProxy->classFloat32Array = classFloat32Array;
	interpreterProxy->classFloat64Array = classFloat64Array;
	interpreterProxy->classLargeNegativeInteger = classLargeNegativeInteger;
	interpreterProxy->classLargePositiveInteger = classLargePositiveInteger;
	interpreterProxy->classPoint = classPoint;
	interpreterProxy->classSemaphore = classSemaphore;
	interpreterProxy->classSmallInteger = classSmallInteger;
	interpreterProxy->classString = classString;
	interpreterProxy->classUnsafeAlien = classUnsafeAlien;
	interpreterProxy->classWordArray = classWordArray;
	interpreterProxy->failed = failed;
	interpreterProxy->falseObject = falseObject;
	interpreterProxy->fetchIntegerofObject = fetchIntegerofObject;
	interpreterProxy->fetchLong32ofObject = fetchLong32ofObject;
	interpreterProxy->fetchPointerofObject = fetchPointerofObject;
	interpreterProxy->firstIndexableField = firstIndexableField;
	interpreterProxy->floatValueOf = floatValueOf;
	interpreterProxy->instanceSizeOf = instanceSizeOf;
	interpreterProxy->instantiateClassindexableSize = instantiateClassindexableSize;
	interpreterProxy->integerObjectOf = integerObjectOf;
	interpreterProxy->integerValueOf = integerValueOf;
	interpreterProxy->ioLoadFunctionFrom = ioLoadFunctionFrom;
	interpreterProxy->isArray = isArray;
	interpreterProxy->isBooleanObject = isBooleanObject;
	interpreterProxy->isBytes = isBytes;
	interpreterProxy->isIntegerObject = isIntegerObject;
	interpreterProxy->isKindOf = isKindOf;
	interpreterProxy->isPinned = isPinned;
	interpreterProxy->isPointers = isPointers;
	interpreterProxy->isPositiveMachineIntegerObject = isPositiveMachineIntegerObject;
	interpreterProxy->isWords = isWords;
	interpreterProxy->isWordsOrBytes = isWordsOrBytes;
	interpreterProxy->majorVersion = majorVersion;
	interpreterProxy->methodArgumentCount = methodArgumentCount;
	interpreterProxy->methodReturnBool = methodReturnBool;
	interpreterProxy->methodReturnFloat = methodReturnFloat;
	interpreterProxy->methodReturnInteger = methodReturnInteger;
	interpreterProxy->methodReturnReceiver = methodReturnReceiver;
	interpreterProxy->methodReturnString = methodReturnString;
	interpreterProxy->methodReturnValue = methodReturnValue;
	interpreterProxy->minorVersion = minorVersion;
	interpreterProxy->nilObject = nilObject;
	interpreterProxy->pop = pop;
	interpreterProxy->popthenPush = popthenPush;
	interpreterProxy->positive32BitIntegerFor = positive32BitIntegerFor;
	interpreterProxy->positive32BitValueOf = positive32BitValueOf;
	interpreterProxy->positive64BitIntegerFor = positive64BitIntegerFor;
	interpreterProxy->positive64BitValueOf = positive64BitValueOf;
	interpreterProxy->primitiveFail = primitiveFail;
	interpreterProxy->primitiveFailFor = primitiveFailFor;
	interpreterProxy->push = push;
	interpreterProxy->pushInteger = pushInteger;
	interpreterProxy->showDisplayBitsLeftTopRightBottom = showDisplayBitsLeftTopRightBottom;
	interpreterProxy->signed32BitIntegerFor = signed32BitIntegerFor;
	interpreterProxy->signed32BitValueOf = signed32BitValueOf;
	interpreterProxy->slotSizeOf = slotSizeOf;
	interpreterProxy->stackIntegerValue = stackIntegerValue;
	interpreterProxy->stackObjectValue = stackObjectValue;
	interpreterProxy->stackValue = stackValue;
	interpreterProxy->statNumGCs = statNumGCs;
	interpreterProxy->storeIntegerofObjectwithValue = storeIntegerofObjectwithValue;
	interpreterProxy->storeLong32ofObjectwithValue = storeLong32ofObjectwithValue;
	interpreterProxy->stringForCString = stringForCString;
	interpreterProxy->stSizeOf = stSizeOf;
	interpreterProxy->success = success;
	interpreterProxy->trueObject = trueObject;

	return interpreterProxy;
}
