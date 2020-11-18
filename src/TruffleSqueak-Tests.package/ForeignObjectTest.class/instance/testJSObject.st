testing
testJSObject
	| jsObject |
	(Polyglot isPermitted: Polyglot languageIdJS) ifFalse: [ ^self ].

	jsObject := (self jsEval: 'new Object({a: "Foo"})').
	
	self deny: (Interop isBoolean: jsObject).
	self should: [ Interop asBoolean: jsObject ] raise: Error.
	self deny: (Interop isNull: jsObject).
	self deny: (Interop isString: jsObject).
	self should: [ Interop asString: jsObject ] raise: Error.
	self deny: (Interop isPointer: jsObject).
	self should: [ Interop asPointer: jsObject ] raise: Error.
	self deny: (Interop fitsInLong: jsObject).
	self should: [ Interop asLong: jsObject ] raise: Error.
	self deny: (Interop fitsInDouble: jsObject).
	self should: [ Interop asDouble: jsObject ] raise: Error.
	self deny: (Interop hasArrayElements: jsObject).
	
	self assert: 1 equals: (Interop getMemberSize: jsObject).
	self assert: (Interop getMemberSize: jsObject)
		equals: (Interop getMembers: jsObject) size.
	self assert: ((Interop getMembers: jsObject) includes: 'a').

	self deny: (Interop hasIdentity: jsObject).
	self assert: (Interop hasMembers: jsObject).
	self assert: (Interop isMemberExisting: jsObject member: 'a').
	self deny: (Interop isMemberInsertable: jsObject member: 'a').
	self deny: (Interop isMemberInternal: jsObject member: 'a').
	self deny: (Interop isMemberInvocable: jsObject member: 'a').
	self assert: (Interop isMemberModifiable: jsObject member: 'a').
	self assert: (Interop isMemberReadable: jsObject member: 'a').
	self assert: (Interop isMemberRemovable: jsObject member: 'a').
	self assert: (Interop isMemberWritable: jsObject member: 'a').
	self assert: 'Foo' equals: (Interop readMember: jsObject member: 'a').
	
	self deny: (Interop isMemberExisting: jsObject member: 'b').
	self assert: (Interop isMemberInsertable: jsObject member: 'b').
	self assert: Smalltalk equals: (Interop writeMember: jsObject member: 'b' value: Smalltalk).
	self assert: ((Interop getMembers: jsObject) includes: 'b').
	self assert: Smalltalk equals: (Interop readMember: jsObject member: 'b').
	self assert: jsObject equals: (Interop removeMember: jsObject member: 'b').
	self deny: (Interop isMemberExisting: jsObject member: 'b')
