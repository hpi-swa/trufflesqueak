primitives members
getMembers: anObject includeInternal: aBoolean
	<primitive: 'primitiveGetMembers' module: 'PolyglotPlugin'>
	^ (self hasMembers: anObject)
		ifTrue: [ anObject interopGetMembers: aBoolean ]
		ifFalse: [ self error: 'Object does not have members' ]