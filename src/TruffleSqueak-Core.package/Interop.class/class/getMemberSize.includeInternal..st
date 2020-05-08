primitives members
getMemberSize: anObject includeInternal: aBoolean
	<primitive: 'primitiveGetMemberSize' module: 'PolyglotPlugin'>
	(self hasMembers: anObject)
		ifFalse: [ self error: 'Object does not have members' ]
		ifTrue: [ self error: 'Unexpected error' ]