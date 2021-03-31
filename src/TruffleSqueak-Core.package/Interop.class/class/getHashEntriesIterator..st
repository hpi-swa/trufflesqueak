primitives hashes
getHashEntriesIterator: anObject
	<primitive: 'primitiveGetHashEntriesIterator' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashEntriesIterator ]
		ifFalse: [ self error: 'Object does not have hash entries' ]