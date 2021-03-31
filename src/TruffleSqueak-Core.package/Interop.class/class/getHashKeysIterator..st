primitives hashes
getHashKeysIterator: anObject
	<primitive: 'primitiveGetHashKeysIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashKeysIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]