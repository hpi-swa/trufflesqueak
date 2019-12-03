primitives
getMembers: anObject
	<primitive: 'primitiveGetMembers' module: 'PolyglotPlugin'>
	(self hasMembers: anObject)
		ifFalse: [ self error: 'Object does not have members' ]
		ifTrue: [ self error: 'Unexpected error' ]