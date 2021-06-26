primitives members
getMembers: anObject includeInternal: aBoolean
	<primitive: 'primitiveGetMembers' module: 'PolyglotPlugin'>
	^ (self hasMembers: anObject)
		ifTrue: [ anObject isForeignObject
			ifTrue: [ PolyglotError signal ]
			ifFalse: [ anObject interopGetMembers: aBoolean ] ]
		ifFalse: [ self error: 'Object does not have members' ]