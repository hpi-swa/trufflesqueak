primitives
getMemberSize: anObject
	<primitive: 'primitiveGetMemberSize' module: 'PolyglotPlugin'>
	(self hasMembers: anObject)
		ifFalse: [ self error: 'Object does not have members' ]
		ifTrue: [ self error: 'Unexpected error' ]