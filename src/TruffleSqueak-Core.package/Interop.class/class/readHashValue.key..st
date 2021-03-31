primitives hashes
readHashValue: anObject key: key
	<primitive: 'primitiveReadHashValue' module: 'PolyglotPlugin'>
	^ (self isHashEntryReadable: anObject key: key)
		ifTrue: [ anObject interopReadHashValue: key ]
		ifFalse: [ self error: 'Key ', key, ' is not readable' ]