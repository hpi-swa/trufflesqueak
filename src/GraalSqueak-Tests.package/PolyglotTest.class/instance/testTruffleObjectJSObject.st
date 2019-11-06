testing
testTruffleObjectJSObject
	| jsObject |
	jsObject := (Polyglot eval: Polyglot languageIdJS string: 'new Object({a: "Foo"})').
	
	self deny: jsObject primitiveIsBoolean.
	self should: [ jsObject primitiveAsBoolean ] raise: Error.
	self deny: jsObject primitiveIsNull.
	self deny: jsObject primitiveIsString.
	self should: [ jsObject primitiveAsString ] raise: Error.
	self deny: jsObject primitiveIsPointer.
	self should: [ jsObject primitiveAsPointer ] raise: Error.
	self deny: jsObject primitiveFitsInLong.
	self should: [ jsObject primitiveAsLong ] raise: Error.
	self deny: jsObject primitiveFitsInDouble.
	self should: [ jsObject primitiveAsDouble ] raise: Error.
	self deny: jsObject primitiveHasArrayElements.
	
	self assert: 1 equals: jsObject primitiveGetMemberSize.
	self assert: jsObject primitiveGetMemberSize equals: jsObject primitiveGetMembers size.
	self assert: (jsObject primitiveGetMembers includes: 'a').

	self assert: jsObject primitiveHasMembers.
	self assert: (jsObject primitiveIsMemberExisting: 'a').
	self deny: (jsObject primitiveIsMemberInsertable: 'a').
	self deny: (jsObject primitiveIsMemberInternal: 'a').
	self deny: (jsObject primitiveIsMemberInvocable: 'a').
	self assert: (jsObject primitiveIsMemberModifiable: 'a').
	self assert: (jsObject primitiveIsMemberReadable: 'a').
	self assert: (jsObject primitiveIsMemberRemovable: 'a').
	self assert: (jsObject primitiveIsMemberWritable: 'a').
	self assert: 'Foo' equals: (jsObject primitiveReadMember: 'a').
	
	self deny: (jsObject primitiveIsMemberExisting: 'b').
	self assert: (jsObject primitiveIsMemberInsertable: 'b').
	self assert: Smalltalk equals: (jsObject primitiveWriteMember: 'b' value: Smalltalk).
	self assert: (jsObject primitiveGetMembers includes: 'b').
	self assert: Smalltalk equals: (jsObject primitiveReadMember: 'b').
	self assert: jsObject equals: (jsObject primitiveRemoveMember: 'b').
	self deny: (jsObject primitiveIsMemberExisting: 'b')
