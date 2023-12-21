#include <stdlib.h>

#define VM_PROXY_MAJOR 1
#define VM_PROXY_MINOR 17
#include "sqVirtualMachine.h"

VirtualMachine* createInterpreterProxy(
    // sorted alphabetically, identical to getExecutables in
    // src/de.hpi.swa.trufflesqueak/src/de/hpi/swa/trufflesqueak/nodes/plugins/ffi/InterpreterProxy.java
	sqInt (*byteSizeOf)(sqInt oop),
	sqInt (*classString)(void),
	sqInt (*failed)(void),
	sqInt (*fetchIntegerofObject)(sqInt fieldIndex, sqInt objectPointer),
	sqInt (*fetchLong32ofObject)(sqInt fieldIndex, sqInt oop),
	sqInt (*fetchPointerofObject)(sqInt index, sqInt oop),
	void *(*firstIndexableField)(sqInt oop),
	double (*floatValueOf)(sqInt oop),
	sqInt (*instantiateClassindexableSize)(sqInt classPointer, sqInt size),
	sqInt (*integerObjectOf)(sqInt value),
	sqInt (*integerValueOf)(sqInt oop),
	void *(*ioLoadFunctionFrom)(char *functionName, char *moduleName),
	sqInt (*isArray)(sqInt oop),
	sqInt (*isBytes)(sqInt oop),
	sqInt (*isPointers)(sqInt oop),
	sqInt (*isPositiveMachineIntegerObject)(sqInt oop),
	sqInt (*isWords)(sqInt oop),
	sqInt (*isWordsOrBytes)(sqInt oop),
	sqInt (*majorVersion)(void),
	sqInt (*methodArgumentCount)(void),
	sqInt (*methodReturnInteger)(sqInt integer),
	sqInt (*methodReturnReceiver)(void),
	sqInt (*methodReturnValue)(sqInt oop),
	sqInt (*minorVersion)(void),
	sqInt (*nilObject)(void),
	sqInt (*pop)(sqInt nItems),
	sqInt (*popthenPush)(sqInt nItems, sqInt oop),
	sqInt (*positive32BitIntegerFor)(unsigned int integerValue),
	usqInt (*positive32BitValueOf)(sqInt oop),
	usqLong (*positive64BitValueOf)(sqInt oop),
	sqInt (*primitiveFail)(void),
	sqInt (*primitiveFailFor)(sqInt reasonCode),
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
	usqInt (*storeLong32ofObjectwithValue)(sqInt fieldIndex, sqInt oop, usqInt anInteger)
) {
	VirtualMachine* interpreterProxy = (VirtualMachine*)calloc(1, sizeof(VirtualMachine));

	interpreterProxy->byteSizeOf = byteSizeOf;
	interpreterProxy->classString = classString;
	interpreterProxy->failed = failed;
	interpreterProxy->fetchIntegerofObject = fetchIntegerofObject;
	interpreterProxy->fetchLong32ofObject = fetchLong32ofObject;
	interpreterProxy->fetchPointerofObject = fetchPointerofObject;
	interpreterProxy->firstIndexableField = firstIndexableField;
	interpreterProxy->floatValueOf = floatValueOf;
	interpreterProxy->instantiateClassindexableSize = instantiateClassindexableSize;
	interpreterProxy->integerObjectOf = integerObjectOf;
	interpreterProxy->integerValueOf = integerValueOf;
	interpreterProxy->ioLoadFunctionFrom = ioLoadFunctionFrom;
	interpreterProxy->isArray = isArray;
	interpreterProxy->isBytes = isBytes;
	interpreterProxy->isPointers = isPointers;
	interpreterProxy->isPositiveMachineIntegerObject = isPositiveMachineIntegerObject;
	interpreterProxy->isWords = isWords;
	interpreterProxy->isWordsOrBytes = isWordsOrBytes;
	interpreterProxy->majorVersion = majorVersion;
	interpreterProxy->methodArgumentCount = methodArgumentCount;
	interpreterProxy->methodReturnInteger = methodReturnInteger;
	interpreterProxy->methodReturnReceiver = methodReturnReceiver;
	interpreterProxy->methodReturnValue = methodReturnValue;
	interpreterProxy->minorVersion = minorVersion;
	interpreterProxy->nilObject = nilObject;
	interpreterProxy->pop = pop;
	interpreterProxy->popthenPush = popthenPush;
	interpreterProxy->positive32BitIntegerFor = positive32BitIntegerFor;
	interpreterProxy->positive32BitValueOf = positive32BitValueOf;
	interpreterProxy->positive64BitValueOf = positive64BitValueOf;
	interpreterProxy->primitiveFail = primitiveFail;
	interpreterProxy->primitiveFailFor = primitiveFailFor;
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

	return interpreterProxy;
}
