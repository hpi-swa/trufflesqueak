primitives hashes
removeHashEntry: anObject key: key
	<primitive: 'primitiveRemoveHashEntry' module: 'PolyglotPlugin'>
	^ (self isHashEntryRemovable: anObject key: key)
		ifTrue: [ anObject interopRemoveHashEntry: key ]
		ifFalse: [ self error: 'Key ', key, ' is not removable' ]