testing
testVMIntrospection
	| testObject |
	Polyglot isPolyglotEvalAllowed ifFalse: [ ^self ].

	testObject := (TruffleSqueakUtilities primitiveGetVMObject: SmalltalkImage).
	"Every Java object understands hashCode"
	self assert: (Interop fitsInLong: testObject hashCode).
	"Ensure that class has a layout"
	self assert: testObject hasLayout.
	"Traverse class and unwrap object"
	self assert: 'Object' equals: testObject getSuperclass toString asString.
	"Allocate Object[]"
	self assert: 10 equals: (testObject getOtherPointers getClass new: 10) size.