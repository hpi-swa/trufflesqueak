#include <stdlib.h>
#include "sqVirtualMachine.h"

VirtualMachine* createInterpreterProxy(
	sqInt (*byteSizeOf)(sqInt oop),
	void *(*firstIndexableField)(sqInt oop),
	sqInt (*isBytes)(sqInt oop),
	sqInt (*majorVersion)(void),
	sqInt (*methodArgumentCount)(void),
	sqInt (*minorVersion)(void),
	sqInt (*primitiveFail)(void),
	sqInt (*stackValue)(sqInt offset)
) {
	VirtualMachine* interpreterProxy = (VirtualMachine*)calloc(1, sizeof(VirtualMachine));
	interpreterProxy->byteSizeOf = byteSizeOf;
	interpreterProxy->firstIndexableField = firstIndexableField;
	interpreterProxy->isBytes = isBytes;
	interpreterProxy->majorVersion = majorVersion;
	interpreterProxy->methodArgumentCount = methodArgumentCount;
	interpreterProxy->minorVersion = minorVersion;
	interpreterProxy->primitiveFail = primitiveFail;
	interpreterProxy->stackValue = stackValue;
	return interpreterProxy;
}
