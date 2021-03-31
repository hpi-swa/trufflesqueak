primitives hashes
readHashValueOrDefault: anObject key: key default: defaultValue
	<primitive: 'primitiveReadHashValueOrDefault' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopReadHashValueOrDefault: key and: defaultValue ]
		ifFalse: [ self error: 'Object does not have hash entries' ]