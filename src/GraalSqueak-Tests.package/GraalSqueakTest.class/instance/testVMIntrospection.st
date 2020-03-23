testing
testVMIntrospection
	| testObject |
	testObject := (GraalSqueakUtilities primitiveGetVMObject: SmalltalkImage).
	"Every Java object understands hashCode"
	self assert: (Interop fitsInLong: testObject hashCode).
	"Invoke method and compare with reading and executing the same method"
	self assert: testObject hashCode asString equals: (Interop readMember: testObject member: 'hashCode') value asString.
	"Ensure that class has a layout"
	self assert: testObject hasLayout wrappedJavaObject.
	"Traverse class and unwrap object"
	self assert: Object equals: testObject superclass wrappedJavaObject.
	"Allocate Object[]"
	self assert: 10 equals: (testObject pointers getClass new: 10) size.