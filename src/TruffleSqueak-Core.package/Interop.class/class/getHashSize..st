primitives hashes
getHashSize: anObject
	<primitive: 'primitiveGetHashSize' module: 'PolyglotPlugin'>
	^ (self hasHashEntries: anObject)
		ifTrue: [ anObject interopGetHashSize ]
		ifFalse: [ self error: 'Object does not have hash entries' ]