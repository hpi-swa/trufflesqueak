#include <stdlib.h>
#include "sqVirtualMachine.h"

VirtualMachine* createInterpreterProxy(
	sqInt (*byteSizeOf)(sqInt oop),
	sqInt (*classString)(void),
	sqInt (*failed)(void),
	void *(*firstIndexableField)(sqInt oop),
	sqInt (*instantiateClassindexableSize)(sqInt classPointer, sqInt size),
	sqInt (*isBytes)(sqInt oop),
	sqInt (*majorVersion)(void),
	sqInt (*methodArgumentCount)(void),
	sqInt (*minorVersion)(void),
	sqInt (*nilObject)(void),
	sqInt (*pop)(sqInt nItems),
	sqInt (*popthenPush)(sqInt nItems, sqInt oop),
	sqInt (*primitiveFail)(void),
	sqInt (*pushInteger)(sqInt integerValue),
	sqInt (*signed32BitIntegerFor)(sqInt integerValue),
	int (*signed32BitValueOf)(sqInt oop),
	sqInt (*stackIntegerValue)(sqInt offset),
	sqInt (*stackValue)(sqInt offset)
) {
	VirtualMachine* interpreterProxy = (VirtualMachine*)calloc(1, sizeof(VirtualMachine));
	interpreterProxy->byteSizeOf = byteSizeOf;
	interpreterProxy->classString = classString;
	interpreterProxy->failed = failed;
	interpreterProxy->firstIndexableField = firstIndexableField;
	interpreterProxy->instantiateClassindexableSize = instantiateClassindexableSize;
	interpreterProxy->isBytes = isBytes;
	interpreterProxy->majorVersion = majorVersion;
	interpreterProxy->methodArgumentCount = methodArgumentCount;
	interpreterProxy->minorVersion = minorVersion;
	interpreterProxy->nilObject = nilObject;
	interpreterProxy->pop = pop;
	interpreterProxy->popthenPush = popthenPush;
	interpreterProxy->primitiveFail = primitiveFail;
	interpreterProxy->pushInteger = pushInteger;
	interpreterProxy->signed32BitIntegerFor = signed32BitIntegerFor;
	interpreterProxy->signed32BitValueOf = signed32BitValueOf;
	interpreterProxy->stackIntegerValue = stackIntegerValue;
	interpreterProxy->stackValue = stackValue;
	return interpreterProxy;
}
