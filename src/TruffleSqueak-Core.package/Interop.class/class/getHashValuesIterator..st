primitives hashes
getHashValuesIterator: anObject
	<primitive: 'primitiveGetHashValuesIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashValuesIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]